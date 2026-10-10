package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Master of Ceremonies — {3}{W} Creature — Rhino Druid 3/4 (New Capenna Commander #18).
 *
 * At the beginning of your upkeep, each opponent chooses money, friends, or secrets. For each
 * player who chose money, you and that player each create a Treasure token. For each player who
 * chose friends, you and that player each create a 1/1 green and white Citizen creature token.
 * For each player who chose secrets, you and that player each draw a card.
 *
 * Modelling: a per-opponent [Effects.ForEachPlayer] whose body is a [Effects.ChooseAction]
 * (the Rottenmouth Viper shape). Inside the loop the context controller is rebound to the
 * iterated opponent, so `EffectTarget.Controller` is "that player" — both the chooser and the
 * player rewarded — while "you" is [Player.ControllerOfSource], the one reference that survives
 * the rebind (it falls back to last-known information if Master of Ceremonies has left).
 */
private val masterYou = EffectTarget.PlayerRef(Player.ControllerOfSource)

private fun citizen(controller: EffectTarget) = Effects.CreateToken(
    power = 1,
    toughness = 1,
    colors = setOf(Color.GREEN, Color.WHITE),
    creatureTypes = setOf("Citizen"),
    controller = controller,
)

val MasterOfCeremonies = card("Master of Ceremonies") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rhino Druid"
    power = 3
    toughness = 4
    oracleText = "At the beginning of your upkeep, each opponent chooses money, friends, or secrets. " +
        "For each player who chose money, you and that player each create a Treasure token. " +
        "For each player who chose friends, you and that player each create a 1/1 green and white " +
        "Citizen creature token. For each player who chose secrets, you and that player each draw a card."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            effect = Effects.ChooseAction(
                choices = listOf(
                    EffectChoice(
                        label = "Money (each of you creates a Treasure)",
                        effect = Effects.CreateTreasure(controller = EffectTarget.Controller) then
                            Effects.CreateTreasure(controller = masterYou)
                    ),
                    EffectChoice(
                        label = "Friends (each of you creates a 1/1 Citizen)",
                        effect = citizen(EffectTarget.Controller) then citizen(masterYou)
                    ),
                    EffectChoice(
                        label = "Secrets (each of you draws a card)",
                        effect = Effects.DrawCards(1, EffectTarget.Controller) then
                            Effects.DrawCards(1, masterYou)
                    ),
                ),
                player = EffectTarget.Controller
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d152cfa8-00b7-4aa0-855e-bbd57a5f4b23.jpg?1783923374"
    }
}
