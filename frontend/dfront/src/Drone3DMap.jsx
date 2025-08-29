import React, { useEffect, useRef } from "react";
import mapboxgl from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";
import "./Drone3DMap.css";
import * as THREE from "three";
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';

mapboxgl.accessToken = "";

export default function Drone3DMap({ drones, showPaths, paths }) {
  const mapContainer = useRef(null);
  const mapRef = useRef(null);
  const threeLayerRef = useRef(null);
  const droneModelRef = useRef(null);
  const mapLoadedRef = useRef(false);

  function gridToLatLng(x, y) {
    const latTop = 52.642572;
    const lngLeft = 1.269615;
    const latBottom = 52.630422;
    const lngRight = 1.321200;
    const gridWidth = 190;
    const gridHeight = 190;
    const lng = lngLeft + (x / gridWidth) * (lngRight - lngLeft);
    const lat = latTop - (y / gridHeight) * (latTop - latBottom);
    return [lng, lat];
  }

  useEffect(() => {
    const loadDroneModel = async () => {
      try {
        const gltfLoader = new GLTFLoader();
        const gltf = await new Promise((resolve, reject) => {
          gltfLoader.load(
            '/models/drone.glb',
            resolve,
            undefined,
            reject
          );
        });

        const droneModel = gltf.scene;
        droneModel.traverse((child) => {
          if (child.isMesh) {
            child.material.side = THREE.DoubleSide;
            child.material.depthWrite = true;
            child.material.depthTest = true;
            if (child.material.isMeshStandardMaterial) {
              child.material.metalness = 0.3;
              child.material.roughness = 0.7;
            }
          }
        });

        droneModel.scale.set(0.25, 0.25, 0.25);
        droneModel.rotation.x = -Math.PI / 2;

        droneModelRef.current = droneModel;
      } catch (error) {
        console.error('Error loading drone model:', error);
        const fallbackGeometry = new THREE.BoxGeometry(2, 0.5, 2);
        const fallbackMaterial = new THREE.MeshLambertMaterial({ color: 0x666666 });
        droneModelRef.current = new THREE.Mesh(fallbackGeometry, fallbackMaterial);
      }
    };

    loadDroneModel();
  }, []);

  useEffect(() => {
    if (!mapContainer.current || mapRef.current) return;

    const bounds = [[1.269615, 52.630422], [1.321200, 52.642572]];
    const map = new mapboxgl.Map({
      container: mapContainer.current,
      style: "mapbox://styles/fbh23equ/cme928cmq00dd01pj03ro93g3",
      center: [1.2973, 52.6309],
      zoom: 16,
      pitch: 60,
      minZoom: 16,
      bearing: -17.6,
      antialias: true,
      maxBounds: bounds
    });

    map.on("load", () => {
      mapLoadedRef.current = true;
      try {
        if (map.getLayer("drone-paths-line")) map.removeLayer("drone-paths-line");
        if (map.getSource("drone-paths")) map.removeSource("drone-paths");
      } catch (e) {  }

      const threeLayer = {
        id: "drones-3d",
        type: "custom",
        renderingMode: "3d",
        onAdd: function (map, gl) {
          this._map = map;
          this.camera = new THREE.Camera();
          this.scene = new THREE.Scene();
          this.renderer = new THREE.WebGLRenderer({
            canvas: map.getCanvas(),
            context: gl,
            antialias: true
          });
          this.renderer.autoClear = false;

          this.objects = new Map(); // id -> { group, currentLng, currentLat, targetLng, targetLat, altMSL }
          this._modelScale = 7;
          this.pathLines = new Map(); 
          this._paths = {}; 
          this._showPaths = false;

          this._buildDroneGroup = () => {
            const group = new THREE.Group();
            const modelContainer = new THREE.Object3D();
            modelContainer.scale.set(1, -1, 1);
            group.add(modelContainer);

            if (droneModelRef.current) {
              const droneClone = droneModelRef.current.clone(true);
              modelContainer.add(droneClone);
            } else {
              const fallbackGeometry = new THREE.BoxGeometry(2, 0.5, 2);
              const fallbackMaterial = new THREE.MeshLambertMaterial({ color: 0x666666 });
              modelContainer.add(new THREE.Mesh(fallbackGeometry, fallbackMaterial));
            }
            return { group, modelContainer };
          };

          this._nodeToLngLat = (n) => {
            if (!n) return null;
            if (Array.isArray(n) && n.length >= 2) {
              const [x, y] = n; return gridToLatLng(x, y);
            }
            if (typeof n === 'object') {
              if (n.lng != null && n.lat != null) return [n.lng, n.lat];
              if (n.x != null && n.y != null) return gridToLatLng(n.x, n.y);
            }
            return null;
          };

          this._getNodesAndIndex = (id, entry) => {
            const spec = this._paths ? this._paths[id] : null;
            let nodes = null; let idx = -1;
            if (!spec) return { nodes: null, idx: -1 };
            if (Array.isArray(spec)) {
              nodes = spec; 
            } else if (typeof spec === 'object') {
              nodes = spec.nodes || spec.points || spec.path || spec.list || null;
              if (typeof spec.index === 'number') idx = spec.index;
              else if (typeof spec.cursor === 'number') idx = spec.cursor;
            }
            if (!Array.isArray(nodes) || nodes.length < 2) return { nodes: null, idx: -1 };
            if (idx >= 0 && idx < nodes.length) return { nodes, idx };
            const cur = mapboxgl.MercatorCoordinate.fromLngLat({ lng: entry.currentLng, lat: entry.currentLat }, entry.altMSL);
            let bestI = 0, bestD = Infinity;
            for (let i = 0; i < nodes.length; i++) {
              const ll = this._nodeToLngLat(nodes[i]);
              if (!ll) continue;
              const mc = mapboxgl.MercatorCoordinate.fromLngLat({ lng: ll[0], lat: ll[1] }, entry.altMSL);
              const dx = mc.x - cur.x, dy = mc.y - cur.y;
              const d2 = dx*dx + dy*dy;
              if (d2 < bestD) { bestD = d2; bestI = i; }
            }
            return { nodes, idx: bestI };
          };

          this._ensurePathLine = (id) => {
            let line = this.pathLines.get(id);
            if (!line) {
              const geom = new THREE.BufferGeometry();
              const mat = new THREE.LineBasicMaterial({ color: 0xff2d55 });
              line = new THREE.Line(geom, mat);
              line.renderOrder = 2;
              this.pathLines.set(id, line);
              this.scene.add(line);
            }
            return line;
          };

          this._rebuildPathFor = (id, entry) => {
            const { nodes, idx } = this._getNodesAndIndex(id, entry);
            const line = this._ensurePathLine(id);
            if (!this._showPaths || !nodes || idx >= nodes.length - 1) {
              line.visible = false; return;
            }
            const tail = nodes.slice(idx + 1);
            const coords = tail.map(this._nodeToLngLat).filter(Boolean);
            if (coords.length < 2) { line.visible = false; return; }
            const pts = new Float32Array(coords.length * 3);
            for (let i = 0; i < coords.length; i++) {
              const ll = coords[i];
              const mc = mapboxgl.MercatorCoordinate.fromLngLat({ lng: ll[0], lat: ll[1] }, entry.altMSL);
              pts[i*3+0] = mc.x; pts[i*3+1] = mc.y; pts[i*3+2] = mc.z || 0;
            }
            line.geometry.setAttribute('position', new THREE.BufferAttribute(pts, 3));
            line.geometry.computeBoundingSphere();
            line.visible = true;
          };

          this.updatePaths = (pathsObj) => {
            this._paths = pathsObj || {};
            for (const [id, entry] of this.objects) this._rebuildPathFor(id, entry);
          };

          this.setShowPaths = (v) => {
            this._showPaths = !!v;
            for (const [id, entry] of this.objects) this._rebuildPathFor(id, entry);
          };
        },
        render: function (gl, matrix) {
          this.camera.projectionMatrix = new THREE.Matrix4().fromArray(matrix);
          let moved = false;

          for (const [id, entry] of this.objects) {
            const t = 0.15; 
            const newLng = entry.currentLng + (entry.targetLng - entry.currentLng) * t;
            const newLat = entry.currentLat + (entry.targetLat - entry.currentLat) * t;

            if (Math.abs(newLng - entry.currentLng) > 1e-7 || Math.abs(newLat - entry.currentLat) > 1e-7) {
              moved = true;
              entry.currentLng = newLng;
              entry.currentLat = newLat;
            }

            const mc = mapboxgl.MercatorCoordinate.fromLngLat({ lng: entry.currentLng, lat: entry.currentLat }, entry.altMSL);
            const scale = mc.meterInMercatorCoordinateUnits();
            entry.group.position.set(mc.x, mc.y, mc.z || 0);
            entry.group.scale.set(scale * this._modelScale, -scale * this._modelScale, scale * this._modelScale);
            if (this._showPaths) this._rebuildPathFor(id, entry);
          }

          this.renderer.resetState();
          this.renderer.render(this.scene, this.camera);
          if (moved) this._map.triggerRepaint();
        },
        upsertDrone: function (id, lng, lat, altitudeMetersAGL = 27) {
          let ground = 0;
          if (this._map?.queryTerrainElevation) {
            const elev = this._map.queryTerrainElevation([lng, lat]);
            ground = Number.isFinite(elev) ? elev : 0;
          }
          const altMSL = ground + altitudeMetersAGL;
          let entry = this.objects.get(id);

          if (!entry) {
            const built = this._buildDroneGroup();
            this.scene.add(built.group);
            entry = {
              group: built.group,
              model: built.modelContainer,
              currentLng: lng,
              currentLat: lat,
              targetLng: lng,
              targetLat: lat,
              altMSL,
              heading: null
            };
            this.objects.set(id, entry);
          } else {
            if (Math.abs(entry.targetLng - lng) < 1e-7 && Math.abs(entry.targetLat - lat) < 1e-7) return;
            entry.targetLng = lng;
            entry.targetLat = lat;
            entry.altMSL = altMSL;
          }
        },
        removeMissing: function (keepIds) {
          for (const [id, entry] of this.objects) {
            if (!keepIds.has(id)) {
              this.scene.remove(entry.group);
              this.objects.delete(id);
              const line = this.pathLines?.get(id);
              if (line) { this.scene.remove(line); this.pathLines.delete(id); }
            }
          }
        }
      };

      map.addLayer(threeLayer);
      threeLayerRef.current = threeLayer;
    });

    mapRef.current = map;
    return () => {
      map.remove();
      mapRef.current = null;
      threeLayerRef.current = null;
    };
  }, []);

  
  useEffect(() => {
    const layer = threeLayerRef.current;
    if (!mapRef.current || !layer) return;
    const keep = new Set(drones.map(d => d.droneid).filter(Boolean));
    layer.removeMissing(keep);

    drones.forEach((d) => {
      if (!d?.droneid) return;
      const [lng, lat] = gridToLatLng(d.x, d.y);
      const agl = d.altAGL ?? 27;
      layer.upsertDrone(d.droneid, lng, lat, agl);
    });
    layer.updatePaths?.(paths);
    layer.setShowPaths?.(showPaths);
  }, [drones, paths, showPaths]);

  return (
    <div className="map-wrapper">
      <div ref={mapContainer} className="map-container" />
    </div>
  );
}
