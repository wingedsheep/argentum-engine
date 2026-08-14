package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Oildeep Gearhulk — Aetherdrift #215
 * {U}{U}{B}{B} · Artifact Creature — Construct · 4/4
 *
 * Lifelink, ward {1}
 * When this creature enters, look at target player's hand. You may choose a card from it. If you
 * do, that player discards that card, then draws a card.
 *
 * The Duress pipeline with three deliberate deviations from its set-mate Gastal Raider:
 *
 *  - It **looks**, it doesn't reveal ([LookAtTargetHandEffect], not `RevealHandEffect`) — only the
 *    Gearhulk's controller sees the hand, so the rest of the table learns nothing.
 *  - It hits **target player**, not target opponent — you may legally point it at yourself, which
 *    turns it into a rummage.
 *  - "You may choose" is [SelectionMode.ChooseUpTo] 1, so declining is expressible, and the
 *    discard-then-draw is wrapped in a [ConditionalOnCollectionEffect] on the selection so the
 *    "if you do" rider holds: no card chosen means no discard *and* no draw. An empty hand takes
 *    the same branch.
 *
 * The draw is the *target player's*, not yours — [Effects.DrawCards] is aimed at the same target,
 * so the Gearhulk trades a card rather than stripping one. The discard is a real discard
 * ([MoveType.Discard]), so discard-matters triggers see it.
 */
val OildeepGearhulk = card("Oildeep Gearhulk") {
    manaCost = "{U}{U}{B}{B}"
    colorIdentity = "UB"
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 4
    oracleText = "Lifelink, ward {1}\n" +
        "When this creature enters, look at target player's hand. You may choose a card from it. " +
        "If you do, that player discards that card, then draws a card."

    keywords(Keyword.LIFELINK)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val player = target("target player", Targets.Player)
        effect = Effects.Composite(
            LookAtTargetHandEffect(player),
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                storeAs = "targetHand"
            ),
            SelectFromCollectionEffect(
                from = "targetHand",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                storeSelected = "chosenCard",
                prompt = "You may choose a card for that player to discard",
                alwaysPrompt = true,
                showAllCards = true
            ),
            ConditionalOnCollectionEffect(
                collection = "chosenCard",
                ifNotEmpty = Effects.Composite(
                    MoveCollectionEffect(
                        from = "chosenCard",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                        moveType = MoveType.Discard
                    ),
                    Effects.DrawCards(1, player)
                )
            )
        )
        description = "When this creature enters, look at target player's hand. You may choose a " +
            "card from it. If you do, that player discards that card, then draws a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "215"
        artist = "Artur Nakhodkin"
        flavorText = "Oil-actuated inventions raised eyebrows after the Invasion, but no one could " +
            "argue against the efficacy of the design."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5e2d09e-b9f5-4a0d-96d3-984b5c2c387d.jpg?1783907855"
    }
}
