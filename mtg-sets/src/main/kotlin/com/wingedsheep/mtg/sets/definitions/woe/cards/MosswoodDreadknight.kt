package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Mosswood Dreadknight // Dread Whispers
 * {1}{G}
 * Creature — Human Knight
 * 3/2
 * Trample
 * When this creature dies, you may cast it from your graveyard as an Adventure until the end of
 * your next turn.
 *
 * Adventure: Dread Whispers — {1}{B}, Sorcery — Adventure
 * You draw a card and you lose 1 life.
 *
 * The dies trigger is a gather → grant pipeline that hands the card in the graveyard a
 * [MayPlayExpiry.UntilEndOfNextTurn] may-play permission restricted to the Adventure face
 * (`castFaceIndex = 0`). Restricting the face is what makes the permission match the oracle text:
 * only Dread Whispers becomes castable, never the creature half. Resolving Dread Whispers then
 * exiles the card and grants the ordinary cast-the-creature-from-exile permission (CR 715.3d), so
 * the two halves chain with no card-specific wiring.
 *
 * The gather reads [CardSource.FromZone] scoped to the trigger controller's graveyard and filtered
 * to the source card itself, rather than a bare [CardSource.Self]: the ability says "from **your**
 * graveyard", so if the card has already left the graveyard (or died under an opponent's control
 * and went to its owner's graveyard elsewhere) the trigger finds nothing and grants nothing, which
 * is what CR 400.7 gives — the object that moved zones is a new object the ability can't see.
 */
val MosswoodDreadknight = card("Mosswood Dreadknight") {
    manaCost = "{1}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 2
    oracleText = "Trample\n" +
        "When this creature dies, you may cast it from your graveyard as an Adventure until " +
        "the end of your next turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.Composite(listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Any.sourceItself()
                ),
                storeAs = "dreadknight"
            ),
            Effects.GrantMayPlayFromExile(
                from = "dreadknight",
                expiry = MayPlayExpiry.UntilEndOfNextTurn,
                castFaceIndex = 0
            )
        ))
    }

    adventure("Dread Whispers") {
        manaCost = "{1}{B}"
        typeLine = "Sorcery — Adventure"
        oracleText = "You draw a card and you lose 1 life. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.Composite(listOf(
                Effects.DrawCards(1),
                Effects.LoseLife(1, EffectTarget.Controller)
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9869ac70-5907-45fa-952c-31aef70c5066.jpg?1783915064"

        ruling("2023-09-01", "You must follow the normal timing permissions and restrictions for Dread Whispers when casting it with the permission of Mosswood Dreadknight's triggered ability. You must pay its mana cost (or, if another effect allows, an alternative cost).")
        ruling("2023-09-01", "If a spell is cast as an Adventure, its controller exiles it instead of putting it into its owner's graveyard as it resolves. For as long as it remains exiled, that player may cast it as a permanent spell.")
        ruling("2023-09-01", "An adventurer card is a permanent card in every zone except the stack, as well as while on the stack if not cast as an Adventure. Ignore its alternative characteristics in those cases.")
    }
}
