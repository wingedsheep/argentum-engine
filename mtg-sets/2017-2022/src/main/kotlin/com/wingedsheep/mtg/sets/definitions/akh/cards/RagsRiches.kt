package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rags // Riches — Amonkhet #222
 *
 * Rags {2}{B}{B} — Sorcery: All creatures get -2/-2 until end of turn.
 * Riches {5}{U}{U} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Each opponent chooses a creature they control. You gain control of those creatures.
 *
 * Riches is Summon: Valefor's per-opponent choice: inside the opponent loop `Player.You` is the
 * choosing opponent, so each picks one of their own creatures (no targeting — hexproof doesn't
 * help). The steal runs after the loop, back in the caster's context, so "you" is the caster; the
 * control change is permanent (ruling).
 */
val RagsRiches = card("Rags // Riches") {
    layout = CardLayout.SPLIT
    colorIdentity = "UB"

    face("Rags") {
        manaCost = "{2}{B}{B}"
        typeLine = "Sorcery"
        oracleText = "All creatures get -2/-2 until end of turn."

        spell {
            effect = Effects.ForEachInGroup(
                filter = GroupFilter.AllCreatures,
                effect = Effects.ModifyStats(-2, -2, EffectTarget.IterationEntity)
            )
        }
    }

    face("Riches") {
        manaCost = "{5}{U}{U}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Each opponent chooses a creature they control. You gain control of those creatures."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Effects.Pipeline {
                val chosen = forEachPlayerCollecting(Player.EachOpponent) {
                    val creatures = gather(GameObjectFilter.Creature, Player.You)
                    listOf(
                        chooseExactly(
                            1,
                            from = creatures,
                            useTargetingUI = true,
                            prompt = "Choose a creature you control for your opponent to gain control of",
                            selectedLabel = "Give",
                        )
                    )
                }.single()
                run(Effects.ForEachInCollection(chosen, Effects.GainControl(EffectTarget.IterationEntity)))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Greg Opalinski"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11d84618-aca9-47dc-ae73-36a2c29f584c.jpg?1783936453"
        ruling("2017-04-18", "Rags affects only creatures on the battlefield at the time it resolves. It won't affect creatures that enter the battlefield or noncreature permanents that become creatures later in the turn.")
        ruling("2017-04-18", "Riches's effect doesn't target. Creatures with hexproof may be lured away this way.")
        ruling("2017-04-18", "Riches's effect lasts indefinitely. It doesn't wear off during the cleanup step.")
        ruling("2017-04-18", "Once Riches begins to resolve, no player may take other actions until it's done. Notably, opponents can't try to remove a creature after choosing it but before you gain control of it.")
        ruling("2017-04-18", "In a multiplayer game, each opponent in turn order chooses a creature they control, if they control any creatures. After each opponent has done so, you gain control of each chosen creature simultaneously.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
        ruling("2017-04-18", "If you cast the first half of a split card with aftermath during your turn, you'll have priority immediately after it resolves. You can cast the half with aftermath from your graveyard before any player can take any other action if it's legal for you to do so.")
    }
}
