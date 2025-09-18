package com.game.voicespells.core.network

import org.json.JSONArray
import org.json.JSONObject

object SyncJsonUtil {
    fun playerStatesToJson(states: List<GameSyncManager.PlayerState>): String {
        val arr = JSONArray()
        states.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("x", it.x)
            obj.put("y", it.y)
            obj.put("z", it.z)
            obj.put("hp", it.hp)
            obj.put("mana", it.mana)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun spellEventsToJson(events: List<GameSyncManager.SpellEvent>): String {
        val arr = JSONArray()
        events.forEach {
            val obj = JSONObject()
            obj.put("casterId", it.casterId)
            obj.put("spellName", it.spellName)
            obj.put("targetX", it.targetX)
            obj.put("targetY", it.targetY)
            obj.put("targetZ", it.targetZ)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun playerStatesFromJson(json: String): List<GameSyncManager.PlayerState> {
        val arr = JSONArray(json)
        val list = mutableListOf<GameSyncManager.PlayerState>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                GameSyncManager.PlayerState(
                    obj.getString("id"),
                    obj.getDouble("x").toFloat(),
                    obj.getDouble("y").toFloat(),
                    obj.getDouble("z").toFloat(),
                    obj.getInt("hp"),
                    obj.getInt("mana")
                )
            )
        }
        return list
    }

    fun spellEventsFromJson(json: String): List<GameSyncManager.SpellEvent> {
        val arr = JSONArray(json)
        val list = mutableListOf<GameSyncManager.SpellEvent>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                GameSyncManager.SpellEvent(
                    obj.getString("casterId"),
                    obj.getString("spellName"),
                    obj.getDouble("targetX").toFloat(),
                    obj.getDouble("targetY").toFloat(),
                    obj.getDouble("targetZ").toFloat()
                )
            )
        }
        return list
    }
}
