//package com.example.savvyclub.data.model
//
//import android.util.Log
//import org.json.JSONArray
//import org.json.JSONObject
//
//fun parsePuzzles(jsonString: String): List<Puzzle> {
//    val puzzles = mutableListOf<Puzzle>()
//    val jsonArray = JSONArray(jsonString)
//
//    for (i in 0 until jsonArray.length()) {
//        val obj = jsonArray.getJSONObject(i)
//
//        val id = obj.optInt("id", -1)
//        val type = if (obj.has("type")) {
//            obj.getString("type")
//        } else {
//            Log.w("PuzzleParser", "⚠️ Puzzle id=$id не имеет поля 'type'")
//            null
//        }
//
//        val q = obj.optString("q", "")
//        val a = obj.optString("a", "")
//
//        val question = obj.optJSONObject("question")?.let { parseLocalizedText(it) } ?: emptyMap()
//        val answer = obj.optJSONObject("answer")?.let { parseLocalizedText(it) } ?: emptyMap()
//
//        puzzles.add(
//            Puzzle(
//                id = id,
//                type = type,
//                q = q,
//                a = a,
//                question = question,
//                answer = answer
//            )
//        )
//    }
//    return puzzles
//}
//
//private fun parseLocalizedText(obj: JSONObject): Map<String, String> {
//    val map = mutableMapOf<String, String>()
//    obj.keys().forEach { key ->
//        map[key] = obj.optString(key, "")
//    }
//    return map
//}
