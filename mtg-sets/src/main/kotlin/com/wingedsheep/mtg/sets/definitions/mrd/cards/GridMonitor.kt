package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Grid Monitor — Mirrodin #183 (canonical printing, only printing)
 * {4} · Artifact Creature — Construct · 4/6
 *
 * You can't cast creature spells.
 *
 * The drawback is the reused [PlayersCantCastSpells] primitive (Grand Abolisher / Brisela
 * family) turned inward: `affected = Player.You` scopes the prohibition to the permanent's own
 * controller, and `spellFilter = GameObjectFilter.Creature` narrows it to creature spells. Read
 * at cast-legality time across every casting zone, so it also stops creature spells cast from
 * exile, the graveyard, or the top of the library — the oracle text restricts the *cast*, not a
 * zone. Artifact creature spells are creature spells too, so Grid Monitor locks out its own kin.
 */
val GridMonitor = card("Grid Monitor") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 6
    oracleText = "You can't cast creature spells."

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.You,
            spellFilter = GameObjectFilter.Creature,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "Arnie Swekel"
        flavorText = "The vedalken protect the Knowledge Pool at any cost."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4ab6c59-cd0f-4c29-8e26-6882dca61fb7.jpg?1783944518"
    }
}
