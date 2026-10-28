package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Skyclave Shade — Zendikar Rising #125
 * {1}{B} · Creature — Shade · 3/1
 *
 * Kicker {2}{B}
 * This creature can't block.
 * If this creature was kicked, it enters with two +1/+1 counters on it.
 * Landfall — Whenever a land you control enters, if this card is in your graveyard and it's your
 * turn, you may cast it from your graveyard this turn.
 *
 * The landfall ability functions only from the graveyard (`triggerZone = GRAVEYARD`), and both
 * halves of "if this card is in your graveyard and it's your turn" are an intervening "if",
 * rechecked on resolution. It grants a this-turn may-play permission over the card where it lies
 * (the Mosswood Dreadknight shape); casting it uses the permission up, so a Shade that dies again
 * later in the turn is a new object needing another landfall (ruling). Kicker may be paid.
 */
val SkyclaveShade = card("Skyclave Shade") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Shade"
    oracleText = "Kicker {2}{B}\n" +
        "This creature can't block.\n" +
        "If this creature was kicked, it enters with two +1/+1 counters on it.\n" +
        "Landfall — Whenever a land you control enters, if this card is in your graveyard and it's " +
        "your turn, you may cast it from your graveyard this turn."
    power = 3
    toughness = 1

    keywordAbility(KeywordAbility.kicker("{2}{B}"))

    staticAbility {
        ability = CantBlock()
    }

    replacementEffect(
        EntersWithCounters(
            count = 2,
            selfOnly = true,
            condition = WasKicked,
        )
    )

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        triggerZone = Zone.GRAVEYARD
        interveningIf = Conditions.All(
            Conditions.SourceInZone(Zone.GRAVEYARD),
            Conditions.IsYourTurn,
        )
        effect = Effects.Pipeline {
            val shade = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Any.sourceItself(),
                )
            )
            run(Effects.GrantMayPlayFromExile(
                from = shade,
                expiry = MayPlayExpiry.EndOfTurn,
            ))
        }
        description = "Landfall — Whenever a land you control enters, if this card is in your graveyard " +
            "and it's your turn, you may cast it from your graveyard this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50bbc0e7-48ce-41e2-a014-1f2eb0427550.jpg?1783929367"
        ruling("2020-09-25", "You must follow the normal timing permissions and restrictions for Skyclave Shade when casting it with the permission of its landfall ability. You must pay its mana cost (or, if another effect allows, an alternative cost). You may pay its kicker cost when casting it this way.")
        ruling("2020-09-25", "After you cast Skyclave Shade from your graveyard, it's considered a new object, and the permission from its landfall ability won't apply to it again even if it returns to your graveyard later in the turn. You'll need to have another land enter the battlefield under your control after it has returned to your graveyard to cast it again.")
        ruling("2020-09-25", "Skyclave Shade's landfall ability triggers only if it's in your graveyard immediately after the land enters the battlefield.")
    }
}
