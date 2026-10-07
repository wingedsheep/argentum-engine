package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

val RuinInTheirWake = card("Ruin in Their Wake") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "Search your library for a basic land card and reveal it. You may put that card onto the battlefield tapped " +
        "if you control a land named Wastes. Otherwise, put that card into your hand. Then shuffle."

    keywords(Keyword.DEVOID)

    spell {
        effect = Effects.Pipeline {
            val library = gather(CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand), search = true)
            val found = chooseUpTo(1, from = library, prompt = "Search your library for a basic land card")
            reveal(found, fromZone = Zone.LIBRARY)
            ifNotEmpty(found) {
                val toHand = Effects.Pipeline { toHand(found) }
                run(Effects.If(
                    condition = Conditions.YouControl(GameObjectFilter.Land.named("Wastes")),
                    then = Effects.May(
                        effect = Effects.Pipeline {
                            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
                        },
                        otherwise = toHand,
                        prompt = "Put the revealed basic land onto the battlefield tapped?"
                    ),
                    otherwise = toHand
                ))
            }
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a0ff591-e4d9-4b62-ac9c-7962fd815226.jpg?1783937903"
    }
}
