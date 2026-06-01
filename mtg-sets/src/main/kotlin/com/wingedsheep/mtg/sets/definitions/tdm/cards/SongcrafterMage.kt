package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Songcrafter Mage
 * {G}{U}{R}
 * Creature — Human Bard
 * 3/2
 *
 * Flash
 * When this creature enters, target instant or sorcery card in your graveyard gains harmonize
 * until end of turn. Its harmonize cost is equal to its mana cost. (You may cast that card from
 * your graveyard for its harmonize cost. You may tap a creature you control to reduce that cost
 * by {X}, where X is its power. Then exile the spell.)
 *
 * The ETB grants Harmonize (CR 702.180) at runtime via [Effects.GrantHarmonize], whose cost
 * defaults to the targeted card's own mana cost. The cast-from-graveyard enumerator and the
 * alternative-payment handler honor the granted harmonize exactly like a printed one through the
 * shared `HarmonizeGrants` resolver; the grant expires during the cleanup step.
 */
val SongcrafterMage = card("Songcrafter Mage") {
    manaCost = "{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Creature — Human Bard"
    power = 3
    toughness = 2
    oracleText = "Flash\n" +
        "When this creature enters, target instant or sorcery card in your graveyard gains " +
        "harmonize until end of turn. Its harmonize cost is equal to its mana cost. (You may " +
        "cast that card from your graveyard for its harmonize cost. You may tap a creature you " +
        "control to reduce that cost by {X}, where X is its power. Then exile the spell.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetObject(
            filter = TargetFilter.InstantOrSorceryInGraveyard.ownedByYou()
        )
        effect = Effects.GrantHarmonize(EffectTarget.ContextTarget(0))
        description = "When this creature enters, target instant or sorcery card in your " +
            "graveyard gains harmonize until end of turn. Its harmonize cost is equal to its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9523bc07-49e5-409c-ae6b-b28e305eef36.jpg?1743204888"
    }
}
