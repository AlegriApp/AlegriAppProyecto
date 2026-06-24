package com.example.myapplication.core.common

import java.util.UUID

/**
 * Genera un UUID v4 como String. Usado como identificador cross-system
 * entre Room (local) y Postgres (Supabase) para Offline First.
 */
fun newUuid(): String = UUID.randomUUID().toString()
