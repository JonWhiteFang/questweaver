package dev.questweaver.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.feature.map.ui.MapState
import dev.questweaver.feature.map.ui.TacticalMap
import dev.questweaver.feature.map.ui.Token

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        var state by remember {
          mutableStateOf(
            MapState(
              w = GRID_WIDTH,
              h = GRID_HEIGHT,
              tileSize = TILE_SIZE,
              blocked = setOf(GridPos(OBSTACLE_X, OBSTACLE_Y), GridPos(OBSTACLE_X2, OBSTACLE_Y)),
              difficult = emptySet(),
              tokens = listOf(
                Token("pc", GridPos(PC_X, PC_Y), isEnemy = false, hpPct = PC_HP),
                Token("gob1", GridPos(ENEMY_X, ENEMY_Y), isEnemy = true, hpPct = ENEMY_HP)
              )
            )
          )
        }
        Scaffold { padding ->
          Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
              "QuestWeaver — Tactical Map (Demo)",
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(PADDING_DP.dp)
            )
            TacticalMap(state, onTap = { })
          }
        }
      }
    }
  }

  companion object {
    private const val GRID_WIDTH = 12
    private const val GRID_HEIGHT = 8
    private const val TILE_SIZE = 64f
    private const val OBSTACLE_X = 4
    private const val OBSTACLE_Y = 4
    private const val OBSTACLE_X2 = 5
    private const val PC_X = 1
    private const val PC_Y = 1
    private const val PC_HP = 0.8f
    private const val ENEMY_X = 6
    private const val ENEMY_Y = 3
    private const val ENEMY_HP = 1f
    private const val PADDING_DP = 12
  }
}
