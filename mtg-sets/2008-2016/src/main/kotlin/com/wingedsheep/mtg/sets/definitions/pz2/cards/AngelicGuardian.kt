package com.wingedsheep.mtg.sets.definitions.pz2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Angelic Guardian — Treasure Chest #70767 (canonical printing; first real printing after the
 * Gift Pack 2018 box promo, reprinted in Core Set 2020 #302 and Arena Beginner Set #2).
 * {4}{W}{W} · Creature — Angel · 5/5
 *
 * Flying.
 * Whenever one or more creatures you control attack, they gain indestructible until end of turn.
 *
 * A single batch trigger (CR 508.3). "They" are the creatures declared as attackers in this
 * combat, so the group is `attackedThisCombat()` — the per-combat marker stamped only at
 * attacker declaration. A creature put onto the battlefield attacking was never declared and
 * gains nothing; a declared attacker removed from combat before resolution still gains it.
 * Only the active player declares attackers, so every creature that attacked this combat is one
 * of the creatures this trigger counted.
 */
val AngelicGuardian = card("Angelic Guardian") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever one or more creatures you control attack, they gain indestructible until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Patterns.Group.grantKeywordToAll(
            Keyword.INDESTRUCTIBLE,
            GroupFilter(GameObjectFilter.Creature.attackedThisCombat()),
        )
        description = "Whenever one or more creatures you control attack, they gain indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70767"
        artist = "Sara Winters"
        flavorText = "\"No single arrow can pierce an entire flock.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6cb2bba-a745-492c-948f-9f8af05a68a4.jpg?1783933919"
    }
}
