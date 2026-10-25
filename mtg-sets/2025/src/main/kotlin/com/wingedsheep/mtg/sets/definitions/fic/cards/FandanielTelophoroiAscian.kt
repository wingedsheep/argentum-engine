package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fandaniel, Telophoroi Ascian
 * {4}{B}
 * Legendary Creature — Elder Wizard
 * 4/5
 * Whenever you cast an instant or sorcery spell, surveil 1.
 * At the beginning of your end step, each opponent may sacrifice a nontoken creature of their
 * choice. Each opponent who doesn't loses 2 life for each instant and sorcery card in your
 * graveyard.
 *
 * Per the ruling, each opponent in turn order chooses up to one nontoken creature, then every
 * chosen creature is sacrificed at once — so the choices are collected across the loop and the
 * sacrifice sits after it. Each opponent who chose nothing is recorded; the instant/sorcery count
 * is read off *your* graveyard outside the per-player loop (inside it "you" is the opponent) and
 * stored before the life loss.
 */
val FandanielTelophoroiAscian = card("Fandaniel, Telophoroi Ascian") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Elder Wizard"
    oracleText = "Whenever you cast an instant or sorcery spell, surveil 1.\n" +
        "At the beginning of your end step, each opponent may sacrifice a nontoken creature of " +
        "their choice. Each opponent who doesn't loses 2 life for each instant and sorcery card " +
        "in your graveyard."
    power = 4
    toughness = 5

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.Surveil(1)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val (chosen, refused) = forEachPlayerCollecting(Player.EachOpponent) {
                val theirs = gather(GameObjectFilter.Creature.nontoken(), Player.You)
                val answer = chooseUpTo(
                    1,
                    from = theirs,
                    useTargetingUI = true,
                    prompt = "You may sacrifice a nontoken creature, or lose 2 life for each " +
                        "instant and sorcery card in Fandaniel's controller's graveyard",
                    selectedLabel = "Sacrifice",
                )
                listOf(answer, storePlayer(onlyIf = Conditions.Not(Conditions.CollectionContainsMatch(answer))))
            }
            sacrifice(chosen)
            val spells = storeNumber(
                DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery)
            )
            run(Effects.ForEachPlayer(refused.asPlayers, Effects.LoseLife(spells.amount * 2, EffectTarget.Controller)))
        }
        description = "At the beginning of your end step, each opponent may sacrifice a nontoken " +
            "creature of their choice. Each opponent who doesn't loses 2 life for each instant and " +
            "sorcery card in your graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Paulius Daščioras"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08c9bf6c-fc52-4565-96d1-951a314e59e5.jpg?1783906359"
        ruling("2025-06-06", "While resolving Fandaniel's second ability, the next opponent in turn order chooses up to one nontoken creature to be sacrificed, then each other opponent in turn order does the same. Then each of the chosen creatures are sacrificed simultaneously. Finally, each opponent who didn't sacrifice a creature this way loses 2 life for each instant and sorcery card in your graveyard.")
        ruling("2025-06-06", "Fandaniel's first ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack.")
    }
}
