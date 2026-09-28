package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invasion of Fiora // Marchesa, Resolute Monarch — March of the Machine #114.
 * {4}{B}{B} · Battle — Siege · defense 4 // Legendary Creature — Human Noble 3/6
 *
 * When this Siege enters, choose one or both —
 * • Destroy all legendary creatures.
 * • Destroy all nonlegendary creatures.
 * // Menace, deathtouch
 * // Whenever Marchesa attacks, remove all counters from up to one target permanent.
 * // At the beginning of your upkeep, if you haven't been dealt combat damage since your last
 * // turn, you draw a card and you lose 1 life.
 *
 * The upkeep line is an intervening-if over
 * [Conditions.YouWereDealtCombatDamageSinceYourLastTurn], which reads a record kept across turn
 * boundaries and cleared only when your own turn ends.
 */
private val InvasionOfFioraFront = card("Invasion of Fiora") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, choose one or both —\n" +
        "• Destroy all legendary creatures.\n" +
        "• Destroy all nonlegendary creatures."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("Destroy all legendary creatures.") {
                    effect = Effects.DestroyAll(GameObjectFilter.Creature.legendary())
                },
                mode("Destroy all nonlegendary creatures.") {
                    effect = Effects.DestroyAll(GameObjectFilter.Creature.nonlegendary())
                },
            ),
            chooseCount = 2,
            minChooseCount = 1,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3af679b-6ee6-4a1d-8ec3-b659bdd90b4a.jpg?1783917011"
        ruling("2023-04-14", "If you choose both modes for Invasion of Fiora's enters-the-battlefield ability, the modes happen in order. First all legendary creatures are destroyed, then all nonlegendary creatures.")
    }
}

private val MarchesaResoluteMonarch = card("Marchesa, Resolute Monarch") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Legendary Creature — Human Noble"
    power = 3
    toughness = 6
    oracleText = "Menace, deathtouch\n" +
        "Whenever Marchesa attacks, remove all counters from up to one target permanent.\n" +
        "At the beginning of your upkeep, if you haven't been dealt combat damage since your " +
        "last turn, you draw a card and you lose 1 life."

    keywords(Keyword.MENACE, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val permanent = target(TargetFilter.Permanent, optional = true)
        effect = Effects.RemoveAllCounters(permanent)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.Not(Conditions.YouWereDealtCombatDamageSinceYourLastTurn)
        effect = Effects.DrawCards(1) then
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/back/b/3/b3af679b-6ee6-4a1d-8ec3-b659bdd90b4a.jpg?1783917011"
    }
}

val InvasionOfFiora: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfFioraFront,
    backFace = MarchesaResoluteMonarch,
)
