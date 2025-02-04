package online.partyrun.partyrunapplication.feature.running.battle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import online.partyrun.partyrunapplication.core.model.battle.RunnerIds
import online.partyrun.partyrunapplication.feature.running.battle.ready.CountdownDialog
import online.partyrun.partyrunapplication.feature.running.battle.ready.BattleReadyScreen
import online.partyrun.partyrunapplication.feature.running.battle.running.BattleRunningScreen

@Composable
fun BattleContentScreen(
    navigateToBattleOnWebSocketError: () -> Unit,
    battleId: String? = "",
    runnerIds: RunnerIds,
    viewModel: BattleContentViewModel = hiltViewModel()
) {
    val battleScreenState by viewModel.battleScreenState.collectAsState()
    val battleUiState by viewModel.battleUiState.collectAsState()

    LaunchedEffect(battleId) {
        battleId?.let {
            viewModel.startBattleStream(
                battleId = it,
                navigateToBattleOnWebSocketError = navigateToBattleOnWebSocketError
            )
        }
    }

    CheckStartTime(battleUiState, battleId, viewModel, runnerIds)

    Content(battleScreenState, battleUiState)
}

@Composable
fun Content(
    battleScreenState: BattleScreenState,
    battleUiState: BattleUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        when (battleScreenState) {
            is BattleScreenState.Ready -> BattleReadyScreen(isConnecting = battleUiState.isConnecting)
            is BattleScreenState.Running -> BattleRunningScreen(battleState = battleUiState.battleState)
        }
    }
}

@Composable
private fun CheckStartTime(
    battleUiState: BattleUiState,
    battleId: String?,
    viewModel: BattleContentViewModel,
    runnerIds: RunnerIds
) {
    when (battleUiState.timeRemaining) {
        in 1..Int.MAX_VALUE -> CountdownDialog(battleUiState.timeRemaining)
        0 -> battleId?.let {
            // 위치 업데이트 시작 및 정지 로직
            StartBattleRunning(battleId, viewModel, runnerIds)
        }
    }
}

@Composable
private fun StartBattleRunning(
    battleId: String?,
    viewModel: BattleContentViewModel,
    runnerIds: RunnerIds
) {
    DisposableEffect(Unit) {
        battleId?.let {
            viewModel.initBattleState(runnerIds) // 러너 데이터를 기반으로 BattleState를 초기화하고 시작
            viewModel.startLocationUpdates(battleId = it)
        }

        onDispose {
            viewModel.stopLocationUpdates()
        }
    }
}
