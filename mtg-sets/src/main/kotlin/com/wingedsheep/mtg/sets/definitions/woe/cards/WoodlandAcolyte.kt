package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Woodland Acolyte // Mend the Wilds
 * {2}{W}
 * Creature — Human Cleric
 * 2/2
 *
 * When this creature enters, draw a card.
 *
 * Adventure: Mend the Wilds — {G}, Instant — Adventure
 * Put target permanent card from your graveyard on top of your library.
 *
 * The Adventure targets in the graveyard, so the requirement carries `zone = Zone.GRAVEYARD` and
 * `ownedByYou()` — ownership, not control, is what "your graveyard" means (CR 404.1). Targeting is
 * mandatory: with no permanent card in your graveyard there's no legal target and the spell can't be
 * cast at all, which also means it never exiles itself and the creature half stays uncastable from
 * exile.
 *
 * "Permanent card" is [GameObjectFilter.Permanent] — artifact, creature, enchantment, land,
 * planeswalker, battle. Note the adventurer-card ruling that cuts the other way here: a card with an
 * Adventure sitting in a graveyard is a *permanent* card (its alternative instant/sorcery
 * characteristics are ignored outside the stack), so Mend the Wilds can return another adventurer.
 */
val WoodlandAcolyte = card("Woodland Acolyte") {
    manaCost = "{2}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
        description = "When this creature enters, draw a card."
    }

    adventure("Mend the Wilds") {
        manaCost = "{G}"
        typeLine = "Instant — Adventure"
        oracleText = "Put target permanent card from your graveyard on top of your library. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val card = target(
                "target permanent card from your graveyard",
                TargetObject(
                    filter = TargetFilter(
                        baseFilter = GameObjectFilter.Permanent.ownedByYou(),
                        zone = Zone.GRAVEYARD,
                    ),
                ),
            )
            effect = Effects.Move(
                target = card,
                destination = Zone.LIBRARY,
                placement = ZonePlacement.Top,
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Steve Prescott"
        flavorText = "He left knighthood behind to heal the scars of the invasion."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9f10623-4783-4773-b9c8-a5a2bcfdb5d9.jpg?1783915061"

        ruling(
            "2023-09-01",
            "An adventurer card is a permanent card in every zone except the stack, as well as while " +
                "on the stack if not cast as an Adventure. Ignore its alternative characteristics in " +
                "those cases."
        )
    }
}
