package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Single Combat
 * {3}{W}{W}
 * Sorcery
 * Each player chooses a creature or planeswalker they control, then sacrifices the rest. Players
 * can't cast creature or planeswalker spells until the end of your next turn.
 *
 * Each player picks their keeper inside `forEachPlayerCollecting` (APNAP); every "rest" is then
 * sacrificed at once, outside the loop — the Promise of Loyalty shape. The cast ban lands on every
 * player with [Duration.EndOfYourNextTurn], so it lifts at the cleanup of the caster's next turn.
 */
val SingleCombat = card("Single Combat") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player chooses a creature or planeswalker they control, then sacrifices the rest. " +
        "Players can't cast creature or planeswalker spells until the end of your next turn."

    spell {
        effect = Effects.Pipeline {
            val (_, rest) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val permanents = gather(
                    CardSource.ControlledPermanents(Player.You, GameObjectFilter.CreatureOrPlaneswalker)
                )
                val (keeper, others) = chooseExactlySplit(
                    1,
                    from = permanents,
                    selectedLabel = "Keep",
                    remainderLabel = "Sacrifice",
                    prompt = "Choose a creature or planeswalker to keep. The rest are sacrificed.",
                    useTargetingUI = true
                )
                listOf(keeper, others)
            }
            sacrifice(rest)
            run(
                Effects.CantCastSpells(
                    EffectTarget.PlayerRef(Player.Each),
                    Duration.EndOfYourNextTurn,
                    GameObjectFilter.CreatureOrPlaneswalker
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Livia Prima"
        flavorText = "Not even a threat to the entire Multiverse could persuade them to put aside their feud."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce0e7c6a-e628-4327-a16f-2062c5a662df.jpg?1783933474"
    }
}
