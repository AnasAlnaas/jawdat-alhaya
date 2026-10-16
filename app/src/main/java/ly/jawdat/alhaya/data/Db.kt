package ly.jawdat.alhaya.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The app's local database, using Android's built-in SQLite directly (no extra library).
 *
 * Dates are stored as ISO text ("2026-10-16"), which sorts correctly and is easy to read in backups.
 * Money is stored as REAL in Libyan dinars.
 * When the schema changes, bump [VERSION] and add a step to [onUpgrade]; never delete user data.
 */
class Db(context: Context) : SQLiteOpenHelper(context, NAME, null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val tables = listOf(
            """
            CREATE TABLE products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                herb TEXT NOT NULL DEFAULT 'plain',
                size TEXT NOT NULL DEFAULT '',
                price REAL NOT NULL DEFAULT 0,
                stock INTEGER NOT NULL DEFAULT 0,
                low_stock INTEGER NOT NULL DEFAULT 0,
                archived INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE markets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                address TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                archived INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE materials (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                unit TEXT NOT NULL DEFAULT '',
                stock REAL NOT NULL DEFAULT 0,
                low_level REAL NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE batches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL,
                name TEXT NOT NULL DEFAULT '',
                herb TEXT NOT NULL DEFAULT 'plain',
                start_date TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'active',
                finished_date TEXT,
                notes TEXT NOT NULL DEFAULT ''
            )
            """,
            """
            CREATE TABLE stages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                batch_id INTEGER NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
                position INTEGER NOT NULL,
                name TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'pending',
                started_date TEXT,
                check_date TEXT,
                done_date TEXT,
                remind INTEGER NOT NULL DEFAULT 1,
                notes TEXT NOT NULL DEFAULT ''
            )
            """,
            "CREATE INDEX stages_batch ON stages(batch_id, position)",
            """
            CREATE TABLE stage_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                stage_id INTEGER NOT NULL REFERENCES stages(id) ON DELETE CASCADE,
                batch_id INTEGER NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
                date TEXT NOT NULL,
                kind TEXT NOT NULL DEFAULT 'other',
                item TEXT NOT NULL,
                qty REAL NOT NULL DEFAULT 0,
                unit TEXT NOT NULL DEFAULT '',
                cost REAL NOT NULL DEFAULT 0,
                material_id INTEGER REFERENCES materials(id) ON DELETE SET NULL
            )
            """,
            "CREATE INDEX items_batch ON stage_items(batch_id)",
            """
            CREATE TABLE stage_photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                stage_id INTEGER NOT NULL REFERENCES stages(id) ON DELETE CASCADE,
                file TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE batch_outputs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                batch_id INTEGER NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
                product_id INTEGER NOT NULL REFERENCES products(id),
                qty INTEGER NOT NULL,
                date TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                market_id INTEGER REFERENCES markets(id),
                product_id INTEGER NOT NULL REFERENCES products(id),
                batch_id INTEGER REFERENCES batches(id) ON DELETE SET NULL,
                qty INTEGER NOT NULL,
                unit_price REAL NOT NULL,
                total REAL NOT NULL,
                notes TEXT NOT NULL DEFAULT ''
            )
            """,
            "CREATE INDEX sales_date ON sales(date)",
            // Every dinar received. A payment made with the sale points to it (sale_id) so editing
            // or deleting the sale can fix it; later "pay what you owe" payments have no sale_id.
            """
            CREATE TABLE payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                market_id INTEGER REFERENCES markets(id),
                sale_id INTEGER REFERENCES sales(id) ON DELETE CASCADE,
                amount REAL NOT NULL,
                notes TEXT NOT NULL DEFAULT ''
            )
            """,
            """
            CREATE TABLE expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                category TEXT NOT NULL DEFAULT 'other',
                description TEXT NOT NULL DEFAULT '',
                amount REAL NOT NULL
            )
            """,
        )
        tables.forEach { db.execSQL(it.trimIndent()) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the first release; add migration steps here.
    }

    companion object {
        const val NAME = "jawdat.db"
        const val VERSION = 1
    }
}
