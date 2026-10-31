package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cavalier of Thorns — Core Set 2020 #167
 * {2}{G}{G}{G} · Creature — Elemental Knight 5/6
 *
 * Reach
 * When this creature enters, reveal the top five cards of your library. Put a land card from among
 * them onto the battlefield and the rest into your graveyard.
 * When this creature dies, you may exile it. If you do, put another target card from your graveyard
 * on top of your library.
 *
 * - The enters trigger is a mandatory gather → exactly-one split: if a land is revealed, one must
 *   go onto the battlefield; with none revealed, all five go to the graveyard.
 * - The dies trigger targets as it goes on the stack ("another" excludes the Cavalier itself), then
 *   Iname, Life Aspect's "you may exile it. If you do" gate: the target moves only if the Cavalier
 *   is actually exiled from the graveyard.
 */
val CavalierOfThorns = card("Cavalier of Thorns") {
    manaCost = "{2}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental Knight"
    power = 5
    toughness = 6
    oracleText = "Reach\n" +
        "When this creature enters, reveal the top five cards of your library. Put a land card from among " +
        "them onto the battlefield and the rest into your graveyard.\n" +
        "When this creature dies, you may exile it. If you do, put another target card from your graveyard " +
        "on top of your library."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(5), revealed = true)
            val (land, rest) = chooseExactlySplit(
                1,
                from = revealed,
                filter = GameObjectFilter.Land,
                prompt = "Put a land card from among them onto the battlefield",
                selectedLabel = "Put onto battlefield",
                remainderLabel = "Put into graveyard",
                showAllCards = true
            )
            move(land, CardDestination.ToZone(Zone.BATTLEFIELD))
            toGraveyard(rest)
        }
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val card = target(
            TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD, excludeSelf = true)
        )
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Move(EffectTarget.Self, Zone.EXILE, fromZone = Zone.GRAVEYARD),
                then = Effects.PutOnTopOfLibrary(card)
            ),
            descriptionOverride = "You may exile Cavalier of Thorns. If you do, put another target card " +
                "from your graveyard on top of your library."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "167"
        artist = "Jehan Choo"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4662a9c-3cbd-41f7-8c15-d048a3826a42.jpg?1783932967"
    }
}
