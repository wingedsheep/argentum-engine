package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Lake-town Mariners // Gone Fishing — The Hobbit #44
 * {4}{U}{U}
 * Creature — Human Citizen
 * 6/5
 *
 * Vigilance
 * Ward {2}
 *
 * Adventure: Gone Fishing — {3}{U}, Instant — Adventure
 * Exile two target creatures and/or lands you control, then return them to the battlefield under
 * their owner's control.
 *
 * The blink is one atomic gather → exile (linked to this spell) → return-from-linked-exile pipeline
 * (Another Round), seeded from [CardSource.ChosenTargets] rather than a player choice because the
 * two permanents are *targets*, locked in as the spell is cast. Both leave and both re-enter as one
 * batch, so enters-the-battlefield abilities see the whole pair — a per-target loop would blink them
 * one at a time and get that wrong.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the creature spell while it remains in exile.)
 */
val LaketownMariners = card("Lake-town Mariners") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Citizen"
    power = 6
    toughness = 5
    oracleText = "Vigilance\n" +
        "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent " +
        "controls, counter it unless that player pays {2}.)"

    keywords(Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    adventure("Gone Fishing") {
        manaCost = "{3}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "Exile two target creatures and/or lands you control, then return them to the " +
            "battlefield under their owner's control. (Then exile this card. You may cast the " +
            "creature later from exile.)"
        spell {
            target(
                "two target creatures and/or lands you control",
                TargetPermanent(
                    count = 2,
                    filter = TargetFilter(GameObjectFilter.CreatureOrLand.youControl())
                )
            )
            effect = Effects.Pipeline(
                descriptionOverride = "Exile two target creatures and/or lands you control, then " +
                    "return them to the battlefield under their owner's control"
            ) {
                val chosen = gather(source = CardSource.ChosenTargets)
                exile(chosen, linkToSource = true)
                val returning = gather(source = CardSource.FromLinkedExile())
                move(returning, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Wei Guan"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4202a678-a5f4-47f9-9c18-e88ab9ad20a4.jpg?1785237929"
    }
}
