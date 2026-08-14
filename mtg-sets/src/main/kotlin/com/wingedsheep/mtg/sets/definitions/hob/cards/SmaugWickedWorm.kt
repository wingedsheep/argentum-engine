package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Smaug, Wicked Worm — The Hobbit #164
 * {3}{B}{R} · Legendary Creature — Dragon · Rare
 * 5/5
 *
 * Flying
 * When Smaug enters, create X tapped Treasure tokens, where X is the number of artifacts your
 * opponents control.
 * Whenever you cast a spell, if mana from a Treasure was spent to cast it, you draw a card and
 * lose 1 life.
 *
 * Modeling notes:
 *  - X counts *opponents'* artifacts, so it reads `Player.EachOpponent` (the same aggregation
 *    Gaea's Avenger uses). It's evaluated on resolution, and X = 0 simply creates nothing.
 *  - "if mana from a Treasure was spent to cast it" is a property of the payment that already
 *    happened, so it rides the trigger itself as
 *    [SpellCastPredicate.PaidWithManaFromSubtype]`(Subtype.TREASURE)` rather than an intervening-if
 *    re-checked on resolution. The engine records the spent-mana provenance on the `SpellCastEvent`
 *    and the cast record, so the check is stable whether or not the spell is still on the stack —
 *    and the ability still triggers for a spell that gets countered.
 *  - The draw and the life loss are a single effect ("you draw a card **and** lose 1 life"), so
 *    they are not independently replaceable and both are mandatory. The life loss targets
 *    `Player.You` explicitly — Smaug's controller, not the spell's.
 */
val SmaugWickedWorm = card("Smaug, Wicked Worm") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When Smaug enters, create X tapped Treasure tokens, where X is the number of artifacts " +
        "your opponents control.\n" +
        "Whenever you cast a spell, if mana from a Treasure was spent to cast it, you draw a card " +
        "and lose 1 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateTreasure(
            count = DynamicAmounts.battlefield(Player.EachOpponent, GameObjectFilter.Artifact).count(),
            tapped = true
        )
        description = "Create X tapped Treasure tokens, where X is the number of artifacts your " +
            "opponents control."
    }

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            requires = setOf(SpellCastPredicate.PaidWithManaFromSubtype(Subtype.TREASURE))
        )
        effect = Effects.DrawCards(1)
            .then(Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You)))
        description = "You draw a card and lose 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "164"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19cc91f0-e724-41ac-b6d8-9a293bd63b42.jpg?1785323313"
    }
}
