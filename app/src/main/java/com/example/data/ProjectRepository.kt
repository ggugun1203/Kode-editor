package com.example.data

import android.content.Context
import com.example.model.Project
import com.example.model.ProjectFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val projectsDir: File
        get() {
            val dir = File(context.filesDir, "droidstudio_projects")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    suspend fun getProjects(): List<Project> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Project>()
        val dirs = projectsDir.listFiles { file -> file.isDirectory } ?: emptyArray()

        for (dir in dirs) {
            val count = countFiles(dir)
            list.add(
                Project(
                    id = dir.name,
                    name = dir.name,
                    path = dir.absolutePath,
                    template = detectTemplate(dir),
                    lastModified = dir.lastModified(),
                    fileCount = count
                )
            )
        }

        // If empty, create initial sample project
        if (list.isEmpty()) {
            val sample = createProject("TokoOnline_Store", "Ecommerce")
            list.add(sample)
        }

        list.sortedByDescending { it.lastModified }
    }

    private fun detectTemplate(dir: File): String {
        val readme = File(dir, "README.md")
        if (readme.exists()) {
            val firstLine = readme.readLines().firstOrNull() ?: ""
            if (firstLine.contains("Store", ignoreCase = true)) return "Ecommerce"
            if (firstLine.contains("Portfolio", ignoreCase = true)) return "Portfolio"
        }
        if (File(dir, "api").exists() || File(dir, "index.php").exists()) return "PHP / Fullstack"
        return "Web"
    }

    private fun countFiles(dir: File): Int {
        var count = 0
        dir.walkTopDown().forEach { if (it.isFile) count++ }
        return count
    }

    suspend fun createProject(name: String, template: String): Project = withContext(Dispatchers.IO) {
        val cleanName = name.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val projectFolder = File(projectsDir, cleanName)
        if (!projectFolder.exists()) projectFolder.mkdirs()

        when (template) {
            "Ecommerce" -> createEcommerceTemplate(projectFolder)
            "Portfolio" -> createPortfolioTemplate(projectFolder)
            "RestApi" -> createRestApiTemplate(projectFolder)
            else -> createBlankTemplate(projectFolder)
        }

        Project(
            id = cleanName,
            name = cleanName,
            path = projectFolder.absolutePath,
            template = template,
            lastModified = System.currentTimeMillis(),
            fileCount = countFiles(projectFolder)
        )
    }

    suspend fun getFileTree(rootPath: String): List<ProjectFile> = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.exists()) return@withContext emptyList()
        buildTree(root)
    }

    private fun buildTree(directory: File): List<ProjectFile> {
        val files = directory.listFiles() ?: return emptyList()
        return files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })).map { file ->
            val ext = if (file.isDirectory) "" else file.extension.lowercase()
            ProjectFile(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                extension = ext,
                size = if (file.isFile) file.length() else 0L,
                lastModified = file.lastModified(),
                children = if (file.isDirectory) buildTree(file) else emptyList()
            )
        }
    }

    suspend fun readFile(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists() && file.isFile) {
            file.readText(Charsets.UTF_8)
        } else {
            ""
        }
    }

    suspend fun writeFile(filePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createFile(parentPath: String, fileName: String, isFolder: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val target = File(parentPath, fileName.trim())
                if (isFolder) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.createNewFile()
                }
            } catch (e: Exception) {
                false
            }
        }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameFile(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(oldPath)
            val newFile = File(file.parentFile, newName.trim())
            file.renameTo(newFile)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun searchProject(rootPath: String, query: String): List<SearchResult> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val results = mutableListOf<SearchResult>()
            val root = File(rootPath)

            root.walkTopDown()
                .filter { it.isFile && !it.name.startsWith(".") && it.length() < 1_000_000 }
                .forEach { file ->
                    try {
                        val lines = file.readLines()
                        lines.forEachIndexed { index, line ->
                            if (line.contains(query, ignoreCase = true)) {
                                results.add(
                                    SearchResult(
                                        filePath = file.absolutePath,
                                        fileName = file.name,
                                        relativePath = file.relativeTo(root).path,
                                        lineNumber = index + 1,
                                        lineContent = line.trim()
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // ignore binary files
                    }
                }
            results
        }

    // --- Starters Templates ---

    private fun createEcommerceTemplate(root: File) {
        val indexHtml = """<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>DroidStore - Toko Online Mobile</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <header>
    <div class="logo">⚡ DroidStore</div>
    <div class="cart-badge">🛒 <span id="cartCount">0</span></div>
  </header>

  <main>
    <section class="hero">
      <h2>Belanja Kebutuhan Gadget & Tech</h2>
      <p>Dibangun & dijalankan langsung di smartphone dengan DroidStudio AI</p>
    </section>

    <section class="products-section">
      <h3>Katalog Produk</h3>
      <div id="productGrid" class="product-grid">
        <!-- Dimuat via JavaScript / API -->
      </div>
    </section>
  </main>

  <div id="cartModal" class="modal">
    <div class="modal-content">
      <span class="close" onclick="toggleCart()">&times;</span>
      <h3>Keranjang Belanja</h3>
      <div id="cartItems"></div>
      <div class="cart-total">Total: <strong id="cartTotal">Rp 0</strong></div>
      <button class="btn-checkout" onclick="checkout()">Checkout Sekarang</button>
    </div>
  </div>

  <footer>
    <p>&copy; 2026 DroidStore. Powered by DroidStudio AI Local Server</p>
  </footer>

  <script src="app.js"></script>
</body>
</html>"""

        val styleCss = """* { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
body { background: #0f172a; color: #f8fafc; min-height: 100vh; }
header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.5rem; background: #1e293b; position: sticky; top: 0; z-index: 10; border-bottom: 1px solid #334155; }
.logo { font-size: 1.25rem; font-weight: bold; color: #38bdf8; }
.cart-badge { background: #38bdf8; color: #0f172a; font-weight: bold; padding: 0.4rem 0.8rem; border-radius: 9999px; cursor: pointer; }
.hero { text-align: center; padding: 2.5rem 1rem; background: linear-gradient(135deg, #1e293b, #0f172a); border-bottom: 1px solid #334155; }
.hero h2 { font-size: 1.75rem; color: #38bdf8; margin-bottom: 0.5rem; }
.hero p { color: #94a3b8; font-size: 0.95rem; }
.products-section { padding: 1.5rem; max-width: 1200px; margin: 0 auto; }
.products-section h3 { margin-bottom: 1rem; font-size: 1.25rem; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 1rem; }
.card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 1rem; display: flex; flex-direction: column; justify-content: space-between; }
.card img { width: 100%; height: 110px; object-fit: cover; border-radius: 8px; margin-bottom: 0.5rem; background: #334155; }
.card h4 { font-size: 1rem; margin-bottom: 0.25rem; }
.card .price { color: #4ade80; font-weight: bold; margin-bottom: 0.75rem; font-size: 0.95rem; }
.btn-buy { background: #0284c7; color: white; border: none; padding: 0.5rem; border-radius: 6px; cursor: pointer; font-weight: 600; width: 100%; transition: background 0.2s; }
.btn-buy:hover { background: #0369a1; }
.modal { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.7); z-index: 50; align-items: center; justify-content: center; padding: 1rem; }
.modal-content { background: #1e293b; border-radius: 12px; padding: 1.5rem; max-width: 400px; width: 100%; border: 1px solid #334155; position: relative; }
.close { position: absolute; right: 1rem; top: 1rem; font-size: 1.5rem; cursor: pointer; color: #94a3b8; }
.cart-total { margin: 1rem 0; font-size: 1.1rem; text-align: right; }
.btn-checkout { background: #10b981; color: white; border: none; padding: 0.75rem; border-radius: 8px; width: 100%; font-size: 1rem; font-weight: bold; cursor: pointer; }
footer { text-align: center; padding: 1.5rem; color: #64748b; font-size: 0.85rem; border-top: 1px solid #334155; margin-top: 2rem; }"""

        val appJs = """const products = [
  { id: 1, name: "Mechanical Keyboard RGB", price: 650000, img: "https://images.unsplash.com/photo-1595225476474-87563907a212?w=300" },
  { id: 2, name: "Wireless Gaming Mouse", price: 320000, img: "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=300" },
  { id: 3, name: "Studio Headset Pro", price: 890000, img: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=300" },
  { id: 4, name: "Ultra-Wide Monitor 27\"", price: 2450000, img: "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=300" }
];

let cart = [];

function renderProducts() {
  const grid = document.getElementById("productGrid");
  if (!grid) return;
  grid.innerHTML = products.map(p => `
    <div class="card">
      <img src="${'$'}{p.img}" alt="${'$'}{p.name}">
      <h4>${'$'}{p.name}</h4>
      <div class="price">Rp ${'$'}{p.price.toLocaleString("id-ID")}</div>
      <button class="btn-buy" onclick="addToCart(${'$'}{p.id})">+ Keranjang</button>
    </div>
  `).join("");
}

function addToCart(productId) {
  const product = products.find(p => p.id === productId);
  if (product) {
    cart.push(product);
    updateCartUI();
  }
}

function updateCartUI() {
  const countBadge = document.getElementById("cartCount");
  if (countBadge) countBadge.innerText = cart.length;

  const cartList = document.getElementById("cartItems");
  const cartTotal = document.getElementById("cartTotal");
  if (cartList) {
    cartList.innerHTML = cart.map((item, idx) => `
      <div style="display:flex; justify-content:space-between; margin:0.5rem 0; font-size:0.9rem;">
        <span>${'$'}{item.name}</span>
        <strong>Rp ${'$'}{item.price.toLocaleString("id-ID")}</strong>
      </div>
    `).join("");
  }
  if (cartTotal) {
    const total = cart.reduce((sum, item) => sum + item.price, 0);
    cartTotal.innerText = `Rp ${'$'}{total.toLocaleString("id-ID")}`;
  }
}

function toggleCart() {
  const modal = document.getElementById("cartModal");
  if (modal) {
    modal.style.display = modal.style.display === "flex" ? "none" : "flex";
  }
}

document.querySelector(".cart-badge")?.addEventListener("click", toggleCart);

function checkout() {
  if (cart.length === 0) {
    alert("Keranjang masih kosong!");
    return;
  }
  alert("Terima kasih! Pesanan sejumlah " + cart.length + " barang berhasil diproses.");
  cart = [];
  updateCartUI();
  toggleCart();
}

window.addEventListener("DOMContentLoaded", renderProducts);
console.log("⚡ DroidStore initialized on local server!");"""

        val apiProductsPhp = """<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");

${'$'}products = [
    ["id" => 1, "name" => "Mechanical Keyboard RGB", "price" => 650000, "stock" => 15],
    ["id" => 2, "name" => "Wireless Gaming Mouse", "price" => 320000, "stock" => 24],
    ["id" => 3, "name" => "Studio Headset Pro", "price" => 890000, "stock" => 8],
    ["id" => 4, "name" => "Ultra-Wide Monitor 27\"", "price" => 2450000, "stock" => 5]
];

echo json_encode([
    "status" => "success",
    "timestamp" => time(),
    "data" => ${'$'}products
]);
?>"""

        val databaseSql = """-- DroidStore Database Schema
CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    price REAL NOT NULL,
    stock INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO products (name, price, stock) VALUES
('Mechanical Keyboard RGB', 650000, 15),
('Wireless Gaming Mouse', 320000, 24),
('Studio Headset Pro', 890000, 8),
('Ultra-Wide Monitor 27\"', 2450000, 5);
"""

        val readme = """# TokoOnline_Store
Aplikasi Web E-Commerce lengkap dibangun menggunakan DroidStudio AI.
- Frontend: HTML5, CSS3, Vanilla JS
- Backend API: PHP & SQLite
- Siap dijalankan di Local Server DroidStudio AI port 8080.
"""

        val workflowYml = """name: Android Build & APK Release

on:
  push:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - name: Checkout Repository
      uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Grant Execute Permission to Gradlew
      run: chmod +x gradlew || true

    - name: Build Debug APK
      run: ./gradlew assembleDebug || echo "Web project packaged"

    - name: Upload APK Artifact
      uses: actions/upload-artifact@v4
      with:
        name: droidstudio-app-debug
        path: app/build/outputs/apk/debug/*.apk
"""

        File(root, "index.html").writeText(indexHtml)
        File(root, "style.css").writeText(styleCss)
        File(root, "app.js").writeText(appJs)
        File(root, "README.md").writeText(readme)

        val apiDir = File(root, "api").apply { mkdirs() }
        File(apiDir, "products.php").writeText(apiProductsPhp)

        val dbDir = File(root, "database").apply { mkdirs() }
        File(dbDir, "schema.sql").writeText(databaseSql)

        val githubDir = File(root, ".github/workflows").apply { mkdirs() }
        File(githubDir, "android-build.yml").writeText(workflowYml)
    }

    private fun createPortfolioTemplate(root: File) {
        val html = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Creative Portfolio</title>
  <style>
    body { background: #121212; color: #fff; font-family: sans-serif; padding: 2rem; }
    h1 { color: #38bdf8; }
  </style>
</head>
<body>
  <h1>Hi, I am a Developer</h1>
  <p>Created directly on Android phone with DroidStudio AI</p>
</body>
</html>"""
        File(root, "index.html").writeText(html)
        File(root, "README.md").writeText("# Developer Portfolio\nMobile created website.")
    }

    private fun createRestApiTemplate(root: File) {
        val php = """<?php
header("Content-Type: application/json");
echo json_encode(["status" => "ok", "message" => "Hello from DroidStudio Local API Server"]);
?>"""
        File(root, "index.php").writeText(php)
        File(root, "README.md").writeText("# REST API Service\nPHP & SQLite Backend")
    }

    private fun createBlankTemplate(root: File) {
        File(root, "index.html").writeText("<!DOCTYPE html>\n<html>\n<head><title>New Project</title></head>\n<body>\n<h1>Hello DroidStudio!</h1>\n</body>\n</html>")
        File(root, "README.md").writeText("# New Project\nEmpty starter project")
    }
}

data class SearchResult(
    val filePath: String,
    val fileName: String,
    val relativePath: String,
    val lineNumber: Int,
    val lineContent: String
)
