package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Decoy Ploy
 * {1}{B}
 * Instant
 *
 * Choose one or both —
 * • Return target Villain card from your graveyard to your hand.
 * • Return target Hero card from your graveyard to your hand.
 *
 * Implementation notes:
 * - "Choose one or both" is the standard `modal(chooseCount = 2, minChooseCount = 1)` shape:
 *   at least one mode must be chosen, up to both, each with its own target picked at cast time.
 * - A card that is both a Villain *and* a Hero can legally be chosen by either mode, and the
 *   two modes may pick different cards (they are separate targets); nothing forbids them from
 *   picking the same card if it has both subtypes, in which case the second return is a no-op.
 */
val DecoyPloy = card("Decoy Ploy") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one or both —\n" +
        "• Return target Villain card from your graveyard to your hand.\n" +
        "• Return target Hero card from your graveyard to your hand."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Return target Villain card from your graveyard to your hand") {
                val villain = target(
                    "target Villain card in your graveyard",
                    TargetObject(
                        filter = TargetFilter(
                            GameObjectFilter.Any.ownedByYou().withSubtype(Subtype.VILLAIN),
                            zone = Zone.GRAVEYARD
                        )
                    )
                )
                effect = Effects.ReturnToHand(villain)
            }
            mode("Return target Hero card from your graveyard to your hand") {
                val hero = target(
                    "target Hero card in your graveyard",
                    TargetObject(
                        filter = TargetFilter(
                            GameObjectFilter.Any.ownedByYou().withSubtype(Subtype.HERO),
                            zone = Zone.GRAVEYARD
                        )
                    )
                )
                effect = Effects.ReturnToHand(hero)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Le Vuong"
        flavorText = "\"What fools you are that you cannot discern between a robotic servant and that which is . . . Doom!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8719b74-48ef-4f68-b59a-949edd644ddc.jpg?1783902945"
    }
}
