import { Database } from "bun:sqlite";

const dbPath = process.env.DB_PATH ?? "slagalica.db";
export const db = new Database(dbPath, { create: true });

db.run("PRAGMA journal_mode = WAL");
db.run("PRAGMA foreign_keys = ON");
db.run("PRAGMA synchronous = NORMAL");
