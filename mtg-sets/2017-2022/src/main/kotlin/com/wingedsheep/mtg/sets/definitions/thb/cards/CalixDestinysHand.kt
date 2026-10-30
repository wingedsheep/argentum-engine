package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Calix, Destiny's Hand
 * {2}{G}{W}
 * Legendary Planeswalker — Calix
 * Loyalty 4
 * +1: Look at the top four cards of your library. You may reveal an enchantment card from among
 * them and put that card into your hand. Put the rest on the bottom of your library in a random order.
 * −3: Exile target creature or enchantment you don't control until target enchantment you control
 * leaves the battlefield.
 * −7: Return all enchantment cards from your graveyard to the battlefield.
 *
 * Modeling notes:
 *  - −3: [Effects.MoveUntilLeaves] anchored to the second target, not to Calix: the exiled card
 *    returns the moment that enchantment leaves, with no trigger, whatever happens to Calix. If the
 *    enchantment is gone as the ability resolves, nothing is exiled (ruling).
 *  - −7: Brilliant Restoration's single gather-then-move batch, so the enchantments enter together
 *    and a returning Aura picks a host already on the battlefield (rulings).
 */
val CalixDestinysHand = card("Calix, Destiny's Hand") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Planeswalker — Calix"
    startingLoyalty = 4
    oracleText = "+1: Look at the top four cards of your library. You may reveal an enchantment card from " +
        "among them and put that card into your hand. Put the rest on the bottom of your library in a random order.\n" +
        "−3: Exile target creature or enchantment you don't control until target enchantment you control " +
        "leaves the battlefield.\n" +
        "−7: Return all enchantment cards from your graveyard to the battlefield."

    loyaltyAbility(+1) {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Enchantment,
            prompt = "You may reveal an enchantment card to put into your hand"
        )
    }

    loyaltyAbility(-3) {
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrEnchantment.opponentControls()))
        val anchor = target(TargetFilter(GameObjectFilter.Enchantment.youControl()))
        effect = Effects.MoveUntilLeaves(victim, Zone.EXILE, anchor = anchor)
        description = "Exile target creature or enchantment you don't control until target enchantment " +
            "you control leaves the battlefield."
    }

    loyaltyAbility(-7) {
        effect = Effects.Pipeline {
            val returning = gather(
                CardSource.FromZone(zone = Zone.GRAVEYARD, player = Player.You, filter = GameObjectFilter.Enchantment)
            )
            move(returning, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
        description = "Return all enchantment cards from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "211"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86458929-ea21-4de8-84d8-a07398ed3bc0.jpg?1783931524"
        ruling(
            "2020-01-24",
            "If the target enchantment you control leaves the battlefield or otherwise becomes an illegal target " +
                "before Calix's second ability resolves, the target permanent you don't control won't be exiled."
        )
        ruling(
            "2020-01-24",
            "Auras attached to the exiled permanent will be put into their owners' graveyards. Any Equipment will " +
                "become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to " +
                "exist. When the card returns to the battlefield, it will be a new object with no connection to the " +
                "card that was exiled."
        )
        ruling("2020-01-24", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling(
            "2020-01-24",
            "If an Aura is put onto the battlefield without being cast, the Aura's controller-to-be chooses what it " +
                "will enchant as it comes back onto the battlefield. An Aura put onto the battlefield this way doesn't " +
                "target anything (so it could be attached to an opponent's permanent with hexproof, for example), but " +
                "the Aura's enchant ability restricts what it can be attached to. If the Aura can't legally be " +
                "attached to anything, it remains in its current zone."
        )
        ruling(
            "2020-01-24",
            "If an Aura is being put onto the battlefield at the same time as an enchantment creature, that Aura " +
                "can't enchant that creature."
        )
    }
}
