package com.github.jan222ik.boardgamenotepad.game

import com.google.gson.annotations.SerializedName
import java.util.stream.Collectors
import kotlin.properties.Delegates

object Game {
    var players: MutableList<Player> by Delegates.observable(mutableListOf()) { _, oldValue, newValue ->
        onPlayersChanged?.invoke(oldValue, newValue)
    }
    var moderator: Player? = null
    var messages: HashMap<Player, String> = hashMapOf()
    var gameState = GameState.JOIN

    var onPlayersChanged: ((MutableList<Player>, MutableList<Player>) -> Unit)? = null

    fun reset() {
        messages = hashMapOf()
        players = mutableListOf()
        gameState = GameState.JOIN
    }

    fun restart() {
        messages = hashMapOf()
        gameState = GameState.JOIN
    }

    fun endJoin(): Boolean {
        val enoughPlayer = players.size > 2
        if (enoughPlayer) {
            gameState = GameState.SELECT_MODERATOR
        }
        return enoughPlayer
    }

    fun addMessage(player: Player, message: String) {
        if (gameState == GameState.ENTER_TEXT) {
            messages[player] = message
            if (messages.values.size >= players.size) {
                gameState = GameState.MODERATOR_PEEK
            }
        } else {
            throw IllegalStateException("You can only join, in Join phase")
        }
    }

    fun pickModerator(player: Player) {
        if (gameState == GameState.SELECT_MODERATOR) {
            moderator = player
            gameState = GameState.ENTER_TEXT
        } else {
            throw IllegalStateException("You can only select a moderator, in Select Moderator phase")
        }
    }

    fun addPlayer(realname: String, id: Int): Int {
        val player = Player(id, realname)
        if (gameState == GameState.JOIN) {
            players.add(player)
            return player.playerId
        } else {
            throw IllegalStateException("You can only join, in Join phase")
        }
    }

    fun queryMessagesModerator(): List<String> {
        if (gameState == GameState.MODERATOR_PEEK) {
            val collect = messages.values.stream().collect(Collectors.toList())
            gameState = GameState.REVEAL
            return collect
        } else {
            throw IllegalStateException("You can only join, in Join phase")
        }
    }

    fun queryMessages(): List<String> {
        if (gameState == GameState.REVEAL) {
            return messages.values.stream().collect(Collectors.toList())
        } else {
            throw IllegalStateException("You can only see the messages, in reveal phase")
        }
    }

    fun toShareAbleState(): SharableGameState =
        SharableGameState(players, moderator, gameState, messages.keys.size)

    fun getMessagesForPlayer(player: Player): List<String>? {
        if (gameState == GameState.MODERATOR_PEEK) {
            if (player == moderator) {
                return messages.map2String()
            } else {
                return null
            }
        } else {
            if (gameState == GameState.REVEAL) {
                return messages.map2String()
            }
            return null
        }
    }
    fun HashMap<Player, String>.map2String() : List<String> {
        val list = mutableListOf<String>()
        this.entries.forEach {
            list.add(it.key.realname.plus("§").plus(it.value))
    }
        return list
    }

    fun reveal(): Boolean {
        return if (gameState == GameState.MODERATOR_PEEK) {
            gameState = GameState.REVEAL
            true
        } else false
    }

    data class SharableGameState(
        @SerializedName("players") val players: List<Player>,
        @SerializedName("moderator") val moderator: Player?,
        @SerializedName("state") val gameState: GameState,
        @SerializedName("msgCount") val msgCount: Int
    )
}
