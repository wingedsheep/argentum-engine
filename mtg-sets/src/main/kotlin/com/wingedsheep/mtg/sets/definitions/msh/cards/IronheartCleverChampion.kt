package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells

/**
 * Ironheart, Clever Champion — Marvel Super Heroes #60 (rare)
 * {4}{U} · Legendary Artifact Creature — Human Hero
 * 3/4
 *
 * Improvise (Your artifacts can help cast this spell. Each artifact you tap after you're done
 * activating mana abilities pays for {1}.)
 * Flying
 * Noncreature spells you cast have improvise.
 *
 * Both halves are plain keyword plumbing on top of the shared tap-for-generic payment rail:
 *  - Printed **improvise** ([Keyword.IMPROVISE], CR 702.126) — the cast enumerator offers the
 *    caster's untapped artifacts, each tap paying {1} of the generic in this spell's total cost.
 *    Note Ironheart is itself an artifact creature, so a board of copies improvises for the next.
 *  - "Noncreature spells you cast have improvise" is [GrantKeywordToOwnSpells] over
 *    [GameObjectFilter.Noncreature] — the same runtime keyword grant Teval uses for delve and
 *    Eirdu for convoke. Every improvise read site goes through the granted-keyword resolver, so a
 *    granted improvise behaves identically to a printed one (and CR 702.126c makes a second
 *    instance redundant, which the boolean check gives for free).
 */
val IronheartCleverChampion = card("Ironheart, Clever Champion") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact Creature — Human Hero"
    power = 3
    toughness = 4
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\n" +
        "Flying\n" +
        "Noncreature spells you cast have improvise."

    keywords(Keyword.IMPROVISE, Keyword.FLYING)

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.IMPROVISE,
            spellFilter = GameObjectFilter.Noncreature
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Julia Vasilyeva"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/395e477a-861f-4661-b329-6c1ad5343ed5.jpg?1783902957"
    }
}
