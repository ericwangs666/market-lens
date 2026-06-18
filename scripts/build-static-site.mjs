import { cpSync, existsSync, mkdirSync, rmSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(process.cwd());
const distDir = resolve(root, "dist");
const assets = [
  "index.html",
  "app.js",
  "config.js",
  "data.js",
  "styles.css",
  "src",
  "daily",
  "realtime",
];

rmSync(distDir, { recursive: true, force: true });
mkdirSync(distDir, { recursive: true });

for (const asset of assets) {
  const source = resolve(root, asset);
  const target = resolve(distDir, asset);
  if (!existsSync(source)) {
    throw new Error(`Missing build asset: ${asset}`);
  }
  cpSync(source, target, { recursive: true });
}

console.log(`Built static frontend into ${distDir}`);
