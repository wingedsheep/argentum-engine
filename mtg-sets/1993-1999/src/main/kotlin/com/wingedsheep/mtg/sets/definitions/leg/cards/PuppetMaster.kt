package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Puppet Master
 * {U}{U}{U}
 * Enchantment — Aura
 * Enchant creature
 * When enchanted creature dies, return that card to its owner's hand. If that card is returned to
 * its owner's hand this way, you may pay {U}{U}{U}. If you do, return this card to its owner's hand.
 *
 * - The dead creature is gathered by its triggering reference, which only names it while it is
 *   still the same object (CR 400.7): a card that left the graveyard first — or a token, which
 *   ceases to exist there — gathers nothing. `moveTracked` records only what actually reached a
 *   hand, and the {U}{U}{U} offer rides `ifNotEmpty` on that record (2008-05-01 ruling).
 * - The hand move routes to the creature card's owner, whoever controlled Puppet Master.
 * - By resolution Puppet Master is already in its owner's graveyard (state-based action), so
 *   `EffectTarget.Self` returns it from there — same shape as Angelic Destiny.
 */
val PuppetMaster = card("Puppet Master") {
    manaCost = "{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When enchanted creature dies, return that card to its owner's hand. If that card is " +
        "returned to its owner's hand this way, you may pay {U}{U}{U}. If you do, return this " +
        "card to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.Pipeline {
            val deadCreature = gather(CardSource.Entity(EffectTarget.TriggeringEntity))
            val returned = moveTracked(deadCreature, CardDestination.ToZone(Zone.HAND))
            ifNotEmpty(returned) {
                run(Effects.MayPay(ManaCost.parse("{U}{U}{U}"), Effects.ReturnToHand(EffectTarget.Self)))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5d26ddc-ad1e-4a97-85fb-34da685c3142.jpg?1783948073"
        ruling(
            "2008-05-01",
            "You only get the opportunity to pay {U}{U}{U} (and thus to return Puppet Master to its " +
                "owner's hand) if the creature card actually gets returned to its owner's hand. If it " +
                "isn't (for instance, if it was removed from the graveyard prior to this ability " +
                "resolving), you don't get the option to pay."
        )
    }
}
