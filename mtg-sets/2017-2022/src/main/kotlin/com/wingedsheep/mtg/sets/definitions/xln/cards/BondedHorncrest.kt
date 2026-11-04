package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.scripting.GameObjectFilter

val BondedHorncrest = card("Bonded Horncrest") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    power = 5
    toughness = 5
    oracleText = "This creature can't attack or block alone."

    staticAbility {
        ability = CantAttackUnlessCoAttacker(coAttackerFilter = GameObjectFilter.Creature)
    }

    staticAbility {
        ability = CantBlockUnlessCoBlocker(coBlockerFilter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Jesper Ejsing"
        flavorText = "\"You don't tame a horncrest. You convince it that it's easier to destroy something else.\"\n—Amautl, knight of Atzocan"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a50ad54a-73f8-4b97-b890-2be9a178bc42.jpg?1783935749"
        ruling("2017-09-29", "If you control more than one Bonded Horncrest, they can attack or block together, even if no other creatures attack or block.")
        ruling("2017-09-29", "Although Bonded Horncrest can’t attack alone, other attacking creatures don’t have to attack the same player or planeswalker. For example, Bonded Horncrest could attack an opponent and another creature could attack a planeswalker that opponent controls. Similarly, other blocking creatures don’t have to block the same creature that Bonded Horncrest blocks.")
        ruling("2017-09-29", "Once Bonded Horncrest has attacked or blocked, it will remain in combat even if you no longer control another attacking or blocking creature.")
        ruling("2017-09-29", "If an effect says that Bonded Horncrest attacks or blocks if able and you control another creature able to attack or block, you must attack or block with Bonded Horncrest and that creature.")
        ruling("2017-09-29", "In a Two-Headed Giant game, Bonded Horncrest can attack or block with a creature controlled by your teammate, even if no other creatures you control are attacking or blocking.")
    }
}
