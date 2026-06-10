import { Database } from "bun:sqlite";

export const db = new Database("slagalica.db", { create: true });

db.run("PRAGMA journal_mode = WAL");
db.run("PRAGMA foreign_keys = ON");
db.run("PRAGMA synchronous = NORMAL");
