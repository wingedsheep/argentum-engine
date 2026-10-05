package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flicker of Fate
 * {1}{W}
 * Instant
 * Exile target creature or enchantment, then return it to the battlefield under its owner's control.
 */
val FlickerOfFate = card("Flicker of Fate") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature or enchantment, then return it to the battlefield under its owner's control."

    spell {
        val permanent = target(TargetFilter.CreatureOrEnchantment)
        effect = Effects.Exile(permanent) then Effects.Move(permanent, Zone.BATTLEFIELD)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Tomasz Jedruszek"
        flavorText = "An infinite journey in an infinitesimal time."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59e19bac-176c-4e37-bfc8-27c00de7c37f.jpg?1783931597"

        ruling("2020-01-24", "Once the exiled permanent returns, it's considered a new object with no relation to the object that it was. Auras attached to the exiled permanent will be put into their owners' graveyards. Equipment attached to the exiled permanent will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist.")
        ruling("2020-01-24", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2020-01-24", "If an Aura is put onto the battlefield without being cast, the Aura's controller-to-be chooses what it will enchant as it comes back onto the battlefield. An Aura put onto the battlefield this way doesn't target anything (so it could be attached to an opponent's permanent with hexproof, for example), but the Aura's enchant ability restricts what it can be attached to. If the Aura can't legally be attached to anything, it remains in its current zone.")
    }
}
