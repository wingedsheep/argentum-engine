package com.wingedsheep.mtg.sets.definitions.duskmourn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Optimistic Scavenger
 * {W}
 * Creature — Human Scout
 * 1/1
 *
 * Eerie — Whenever an enchantment you control enters and whenever you fully unlock a
 * Room, put a +1/+1 counter on target creature.
 */
val OptimisticScavenger = card("Optimistic Scavenger") {
    manaCost = "{W}"
    typeLine = "Creature — Human Scout"
    power = 1
    toughness = 1
    oracleText = "Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room, put a +1/+1 counter on target creature."

    keywords(Keyword.EERIE)

    // Eerie trigger — part 1: whenever an enchantment you control enters
    triggeredAbility {
        trigger = Triggers.AnyEnchantmentYouControlEnters
        val targetCreature = target("target creature", Targets.Creature)
        effect = Effects.AddCounters("+1/+1", 1, targetCreature)
        description = "Eerie — Whenever an enchantment you control enters, put a +1/+1 counter on target creature."
    }

    // Eerie trigger — part 2: whenever you fully unlock a Room
    triggeredAbility {
        trigger = Triggers.RoomFullyUnlocked
        val targetCreature = target("target creature", Targets.Creature)
        effect = Effects.AddCounters("+1/+1", 1, targetCreature)
        description = "Eerie — Whenever you fully unlock a Room, put a +1/+1 counter on target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Brian Valeza"
        flavorText = "\"A void rod? Perfect! Now I'll be able to get the glitch reader working again.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c72fc6f-6a96-420d-812d-b5cf0f57cc7f.jpg?1726285934"
    }
}
