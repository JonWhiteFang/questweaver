package dev.questweaver.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.questweaver.feature.map.ui.TacticalMap
import dev.questweaver.feature.map.ui.MapState
import dev.questweaver.feature.map.ui.GridPos
import dev.questweaver.feature.map.ui.Token

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        var state by remember {
          mutableStateOf(
            MapState(
              w = 12, h = 8, tileSize = 64f,
              blocked = setOf(GridPos(4,4), GridPos(5,4)),
              difficult = emptySet(),
              tokens = listOf(
                Token("pc", GridPos(1,1), isEnemy = false, hpPct = 0.8f),
                Token("gob1", GridPos(6,3), isEnemy = true, hpPct = 1f)
              )
            )
          )
        }
        Scaffold { padding ->
          Column(Modifier.fillMaxSize().padding(padding)) {
            Text("QuestWeaver — Tactical Map (Demo)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(12.dp))
            TacticalMap(state, onTap = { })  // demo only
          }
        }
      }
    }
  }
}
