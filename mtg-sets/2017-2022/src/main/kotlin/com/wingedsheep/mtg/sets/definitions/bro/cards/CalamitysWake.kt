package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.CardSource

val CalamitysWake = card("Calamity's Wake") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile all graveyards. Players can't cast noncreature spells this turn. Exile Calamity's Wake."

    spell {
        effect = Effects.Pipeline {
            val graveyards = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each))
            exile(graveyards)
            run(Effects.CantCastSpells(
                EffectTarget.PlayerRef(Player.Each),
                spellFilter = GameObjectFilter.Noncreature
            ))
        }
        selfExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Slawomir Maniak"
        flavorText = "The sylex's blast had razed the land, leaving behind only snow and sorrow. Urza stood alone in the sudden silence."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/013bed2b-25db-4dfc-9283-e80c9ac6c841.jpg?1783920135"
    }
}
