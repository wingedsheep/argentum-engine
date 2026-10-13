package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elvish Hydromancer
 * {2}{G}
 * Creature — Elf Wizard
 * 3/2
 * Kicker {3}{U}
 * When this creature enters, if it was kicked, create a token that's a copy of target creature you control.
 *
 * The token copy is never itself kicked (it wasn't cast), so targeting Hydromancer itself
 * doesn't loop — the copy's own enters trigger fails its intervening-if.
 */
val ElvishHydromancer = card("Elvish Hydromancer") {
    manaCost = "{2}{G}"
    colorIdentity = "GU"
    typeLine = "Creature — Elf Wizard"
    power = 3
    toughness = 2
    oracleText = "Kicker {3}{U} (You may pay an additional {3}{U} as you cast this spell.)\n" +
        "When this creature enters, if it was kicked, create a token that's a copy of target creature you control."

    keywordAbility(KeywordAbility.kicker("{3}{U}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.CreateTokenCopyOfTarget(target = creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Fajareka Setiawan"
        flavorText = "\"Water remembers every living thing that it has ever been a part of. We need only bring that recollection to the surface.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90dc4122-4f7a-4d9f-8427-de36e3e8a913.jpg?1783921302"
        ruling("2022-09-09", "If it was kicked, Elvish Hydromancer's ability can target any creature you control, including itself. If it targets itself, the token that is created won't have been kicked, so the ability will not trigger again.")
    }
}
