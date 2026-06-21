package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Fire Magic
 * {R}
 * Instant
 *
 * Tiered (Choose one additional cost.)
 * • Fire — {0} — Fire Magic deals 1 damage to each creature.
 * • Fira — {2} — Fire Magic deals 2 damage to each creature.
 * • Firaga — {5} — Fire Magic deals 3 damage to each creature.
 *
 * Tiered (CR 702.183): a choose-one modal spell where the chosen tier's additional mana cost is
 * paid at cast. Each tier scales the sweep damage. No targets.
 */
val FireMagic = card("Fire Magic") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Tiered (Choose one additional cost.)\n" +
        "• Fire — {0} — Fire Magic deals 1 damage to each creature.\n" +
        "• Fira — {2} — Fire Magic deals 2 damage to each creature.\n" +
        "• Firaga — {5} — Fire Magic deals 3 damage to each creature."

    spell {
        tiered {
            tier("Fire", "{0}", "Fire Magic deals 1 damage to each creature.") {
                effect = Patterns.Group.dealDamageToAll(1, GroupFilter.AllCreatures)
            }
            tier("Fira", "{2}", "Fire Magic deals 2 damage to each creature.") {
                effect = Patterns.Group.dealDamageToAll(2, GroupFilter.AllCreatures)
            }
            tier("Firaga", "{5}", "Fire Magic deals 3 damage to each creature.") {
                effect = Patterns.Group.dealDamageToAll(3, GroupFilter.AllCreatures)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Toni Infante"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/415ff6a5-61ef-4b37-ae08-e44476300d4a.jpg?1748706272"
    }
}
