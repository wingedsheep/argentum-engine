package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Windrider Wizard — Zendikar Rising #87
 * {2}{U} · Creature — Human Wizard · 2/2
 *
 * Flying
 * Whenever you cast an instant, sorcery, or Wizard spell, you may draw a card. If you do, discard a card.
 *
 * "May draw; if you do, discard" is [Effects.May] over `Patterns.Hand.loot()`, so declining
 * skips both the draw and the discard.
 */
val WindriderWizard = card("Windrider Wizard") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "Flying\n" +
        "Whenever you cast an instant, sorcery, or Wizard spell, you may draw a card. If you do, discard a card."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery or GameObjectFilter.Any.withSubtype(Subtype.WIZARD)
        )
        effect = Effects.May(Patterns.Hand.loot())
        description = "Whenever you cast an instant, sorcery, or Wizard spell, you may draw a card. " +
            "If you do, discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Slawomir Maniak"
        flavorText = "\"It's an ideal method for exploring sky ruins. The drakes sniff me briefly but then " +
            "lose interest.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c790a86d-db91-4a87-aa3a-3f9fc4d4b662.jpg?1783929384"
        ruling("2020-09-25", "You draw a card and discard a card while Windrider Wizard's last ability is resolving. No player may take actions between those events, and nothing can happen.")
        ruling("2020-09-25", "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger, but after targets have been chosen for that spell (if it has any targets). The ability resolves even if that spell is countered.")
        ruling("2020-09-25", "A Wizard spell is one with the creature type Wizard. Spells that are Wizard-themed (such as Relic Amulet) aren't Wizard spells.")
    }
}
