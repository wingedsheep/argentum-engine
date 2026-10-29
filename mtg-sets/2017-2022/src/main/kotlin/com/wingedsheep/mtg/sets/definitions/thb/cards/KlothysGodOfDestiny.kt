package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Klothys, God of Destiny — Theros Beyond Death #220
 * {1}{R}{G} · Legendary Enchantment Creature — God · 4/5
 *
 * Indestructible
 * As long as your devotion to red and green is less than seven, Klothys isn't a creature.
 * At the beginning of your first main phase, exile target card from a graveyard. If it was a land
 * card, add {R} or {G}. Otherwise, you gain 2 life and Klothys deals 2 damage to each opponent.
 *
 * The devotion clause is the two-colour God template (Xenagos). The trigger is Scavenging Ooze's
 * exile-then-branch on the exiled card's printed type (`TargetMatchesFilter(Land)` — an artifact
 * land is a land card, so it adds mana and nothing else — ruling). Because it targets, the
 * ability uses the stack and isn't a mana ability; the {R}-or-{G} choice is
 * `AddManaOfChoice(Specific(R, G))` made as it resolves.
 */
val KlothysGodOfDestiny = card("Klothys, God of Destiny") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Enchantment Creature — God"
    power = 4
    toughness = 5
    oracleText = "Indestructible\n" +
        "As long as your devotion to red and green is less than seven, Klothys isn't a creature.\n" +
        "At the beginning of your first main phase, exile target card from a graveyard. If it was a land " +
        "card, add {R} or {G}. Otherwise, you gain 2 life and Klothys deals 2 damage to each opponent."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.RED, Color.GREEN),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        val exiled = target(TargetFilter.CardInGraveyard)
        effect = Effects.Exile(exiled) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Land, exiled),
                then = Effects.AddManaOfChoice(ManaColorSet.Specific(setOf(Color.RED, Color.GREEN))),
                otherwise = Effects.GainLife(2) then
                    Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)),
            )
        description = "At the beginning of your first main phase, exile target card from a graveyard. If it " +
            "was a land card, add {R} or {G}. Otherwise, you gain 2 life and Klothys deals 2 damage to each opponent."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "220"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b3c393c-3596-4bd9-a553-e0b03c2eb950.jpg?1783931520"
        ruling(
            "2020-01-24",
            "If the target card in a graveyard is an illegal target by the time Klothys's last ability tries to " +
                "resolve, the ability won't resolve. You won't add mana, gain life, or deal damage."
        )
        ruling(
            "2020-01-24",
            "If you exile a land card that has another card type (such as an artifact land), you add one mana, " +
                "but you won't gain life or deal damage."
        )
        ruling(
            "2020-01-24",
            "If you exile a nonland card, you gain only 2 life, no matter how many opponents are dealt damage."
        )
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
    }
}
