package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells

/**
 * Pestilent Spirit
 * {2}{B}
 * Creature — Spirit
 * 3/2
 * Menace, deathtouch
 * Instant and sorcery spells you control have deathtouch.
 *
 * The spell clause is [GrantKeywordToOwnSpells] over [GameObjectFilter.InstantOrSorcery], the same
 * shape as Heartflame Duelist's lifelink. A spell on the stack isn't projected, so noncombat damage
 * reads deathtouch off the spell-grant channel (`DamageUtils.sourceHasDeathtouch` →
 * `GrantedKeywordResolver`), resolved live against the spell's controller — "spells you control".
 */
val PestilentSpirit = card("Pestilent Spirit") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 2
    oracleText = "Menace, deathtouch\n" +
        "Instant and sorcery spells you control have deathtouch. " +
        "(Any amount of damage they deal to a creature is enough to destroy it.)"

    keywords(Keyword.MENACE, Keyword.DEATHTOUCH)

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.DEATHTOUCH,
            spellFilter = GameObjectFilter.InstantOrSorcery
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Anastasia Ovchinnikova"
        flavorText = "Its stench corrodes steel. Its miasma sickens angels. Its finality ends hope."
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f9a0387-5116-484b-bb2b-064bd42e7fff.jpg?1783933691"
    }
}
