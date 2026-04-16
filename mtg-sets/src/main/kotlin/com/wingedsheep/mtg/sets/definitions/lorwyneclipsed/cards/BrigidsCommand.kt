package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Brigid's Command
 * {1}{G}{W}
 * Kindred Sorcery — Kithkin
 *
 * Choose two —
 * • Create a token that's a copy of target Kithkin you control.
 * • Target player creates a 1/1 green and white Kithkin creature token.
 * • Target creature you control gets +3/+3 until end of turn.
 * • Target creature you control fights target creature an opponent controls.
 */
val BrigidsCommand = card("Brigid's Command") {
    manaCost = "{1}{G}{W}"
    typeLine = "Kindred Sorcery — Kithkin"
    oracleText = "Choose two —\n" +
        "• Create a token that's a copy of target Kithkin you control.\n" +
        "• Target player creates a 1/1 green and white Kithkin creature token.\n" +
        "• Target creature you control gets +3/+3 until end of turn.\n" +
        "• Target creature you control fights target creature an opponent controls."

    spell {
        modal(chooseCount = 2) {
            mode("Create a token that's a copy of target Kithkin you control") {
                val kithkin = target(
                    "target Kithkin you control",
                    TargetObject(filter = TargetFilter(GameObjectFilter.Creature.youControl().withSubtype("Kithkin")))
                )
                effect = Effects.CreateTokenCopyOfTarget(kithkin)
            }
            mode("Target player creates a 1/1 green and white Kithkin creature token") {
                val player = target("target player", TargetPlayer())
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN, Color.WHITE),
                    creatureTypes = setOf("Kithkin"),
                    controller = player,
                    imageUri = "https://cards.scryfall.io/normal/front/2/e/2ed11e1b-2289-48d2-8d96-ee7e590ecfd4.jpg?1767955680"
                )
            }
            mode("Target creature you control gets +3/+3 until end of turn") {
                val creature = target("target creature you control", Targets.CreatureYouControl)
                effect = Effects.ModifyStats(3, 3, creature)
            }
            mode("Target creature you control fights target creature an opponent controls") {
                val yourCreature = target("target creature you control", Targets.CreatureYouControl)
                val theirCreature = target("target creature an opponent controls", Targets.CreatureOpponentControls)
                effect = Effects.Fight(yourCreature, theirCreature)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Sam Guay"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf034777-3da7-4d5c-9213-5e9d235c315a.jpg?1767862610"

        ruling("2025-11-17", "The token created by the first mode of Brigid's Command copies exactly what was printed on the original permanent and nothing else (unless that permanent is copying something else or is a token). It doesn't copy whether that permanent is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
        ruling("2025-11-17", "If the copied permanent is copying something else, then the token enters as whatever that permanent copied.")
        ruling("2025-11-17", "If the copied permanent has {X} in its mana cost, X is 0.")
        ruling("2025-11-17", "If the copied permanent is a token, the token that's created copies the original characteristics of that token as stated by the effect that created that token.")
        ruling("2025-11-17", "Any enters abilities of the copied permanent will trigger when the token enters. Any \"as [this permanent] enters\" or \"[this permanent] enters with\" abilities of the copied permanent will also work.")
        ruling("2025-11-17", "If all of Brigid's Command's targets are illegal as it tries to resolve, it will do nothing. If at least one target is still legal, it will resolve and do as much as it can.")
    }
}
