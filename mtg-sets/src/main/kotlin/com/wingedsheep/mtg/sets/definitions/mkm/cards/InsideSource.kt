package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Inside Source — Murders at Karlov Manor #19
 * {2}{W} · Creature — Human Citizen · 1/1
 *
 * When this creature enters, create a 2/2 white and blue Detective creature token.
 * {3}, {T}: Target Detective you control gets +2/+0 and gains vigilance until end of turn.
 * Activate only as a sorcery.
 *
 * Two Detectives for three mana, then a mana sink that turns one of them into an attacker who
 * stays home. The pump targets *a Detective you control* — including the token it just made, and
 * including Inside Source itself only if something has made it a Detective, since a Human Citizen
 * isn't one. `TargetFilter.CreatureYouControl.withSubtype(DETECTIVE)` is evaluated against
 * projected state, so a creature that gained the type from a type-changing effect is a legal
 * target and one that lost it is not.
 *
 * "Activate only as a sorcery" is [TimingRule.SorcerySpeed] — your main phase, empty stack. Note
 * this bars the usual "pump at end of the opponent's turn to blank a blocker" line; the vigilance
 * half is the compensation, letting the pumped Detective attack and still block back.
 *
 * The token gets no baked-in `imageUri`: a 2/2 white-and-blue Detective is one of MKM's printed
 * tokens, so the set's `tokenArt` layer supplies the art (same as [MuseumNightwatch]).
 */
val InsideSource = card("Inside Source") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Citizen"
    oracleText = "When this creature enters, create a 2/2 white and blue Detective creature token.\n" +
        "{3}, {T}: Target Detective you control gets +2/+0 and gains vigilance until end of turn. " +
        "Activate only as a sorcery."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE, Color.BLUE),
            creatureTypes = setOf("Detective")
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        val detective = target(
            "target Detective you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl.withSubtype(Subtype.DETECTIVE))
        )
        effect = Effects.Composite(
            Effects.ModifyStats(2, 0, detective),
            Effects.GrantKeyword(Keyword.VIGILANCE, detective)
        )
        description = "Target Detective you control gets +2/+0 and gains vigilance until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "John Stanko"
        flavorText = "\"I was never here.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/5/1548d181-3f83-457a-b2d3-eb88cfb2afda.jpg?1783912923"
    }
}
