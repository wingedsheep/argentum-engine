package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ketramose, the New Dawn — Aetherdrift #209.
 *
 * The attack/block gate counts *every* card in exile, whoever owns it and however it got there
 * (Scryfall ruling 2025-02-07) — hence `Count(Player.Each, Zone.EXILE)` rather than a
 * controller-scoped count. `CantAttackUnless` / `CantBlockUnless` are checked at declaration time
 * only, which is also what the second ruling wants: once Ketramose has been declared, it keeps
 * attacking or blocking even if exile shrinks below seven.
 *
 * The draw trigger is a CR 603.2c batch — one exile event fires it once no matter how many cards
 * moved, and "graveyards and/or the battlefield" is unscoped (any graveyard, anyone's permanents),
 * so it uses [Triggers.CardsPutIntoExile] rather than the controller-scoped graveyard batches.
 * "During your turn" is the trigger condition.
 */
val KetramoseTheNewDawn = card("Ketramose, the New Dawn") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — God"
    power = 4
    toughness = 4
    oracleText = "Menace, lifelink, indestructible\n" +
        "Ketramose can't attack or block unless there are seven or more cards in exile.\n" +
        "Whenever one or more cards are put into exile from graveyards and/or the battlefield " +
        "during your turn, you draw a card and lose 1 life."

    keywords(Keyword.MENACE, Keyword.LIFELINK, Keyword.INDESTRUCTIBLE)

    val sevenOrMoreCardsInExile = Compare(
        DynamicAmount.Count(Player.Each, Zone.EXILE),
        ComparisonOperator.GTE,
        DynamicAmount.Fixed(7)
    )

    staticAbility {
        ability = CantAttackUnless(sevenOrMoreCardsInExile)
    }
    staticAbility {
        ability = CantBlockUnless(sevenOrMoreCardsInExile)
    }

    triggeredAbility {
        trigger = Triggers.CardsPutIntoExile()
        triggerCondition = Conditions.IsYourTurn
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.LoseLife(1, EffectTarget.Controller)
        )
        description = "Whenever one or more cards are put into exile from graveyards and/or the " +
            "battlefield during your turn, you draw a card and lose 1 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "209"
        artist = "Maaz Ali Khan"
        flavorText = "Let the past die so the future might live."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cffae8d0-7b4e-42ed-8124-24a86b38f490.jpg?1783907856"
        ruling(
            "2025-02-07",
            "The ability that prevents Ketramose from attacking or blocking counts the total " +
                "number of cards in exile, regardless of who owns them or how they were exiled."
        )
        ruling(
            "2025-02-07",
            "Once Ketramose has been declared as an attacker or blocker, it will continue to " +
                "attack or block that combat even if the number of cards in exile falls below seven."
        )
    }
}
