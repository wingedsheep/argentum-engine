package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.YouControlSource
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Gwen Stacy // Ghost-Spider — Marvel's Spider-Man #78 (mythic)
 *
 * Front — Gwen Stacy · {1}{R} · Legendary Creature — Human Performer Hero · 2/1
 *   When Gwen Stacy enters, exile the top card of your library. You may play that card for as long
 *   as you control this creature.
 *   {2}{U}{R}{W}: Transform Gwen Stacy. Activate only as a sorcery.
 *
 * Back — Ghost-Spider · Legendary Creature — Spider Human Hero · 4/4
 *   Flying, vigilance, haste
 *   Whenever you play a land from exile or cast a spell from exile, put a +1/+1 counter on
 *   Ghost-Spider.
 *   Remove two counters from Ghost-Spider: Exile the top card of your library. You may play that
 *   card this turn.
 *
 * Modeled as a transforming double-faced creature ([CardDefinition.doubleFacedCreature]); the front
 * owns the sorcery-speed [TransformEffect] flip. The back is a transformed face reached only via the
 * flip, so it carries no castable mana cost — its R/U/W colors come from a color indicator (CR 204),
 * `colorIndicator = "RUW"` — mirroring the shipped Miles Morales // Ultimate Spider-Man.
 *
 *  - ETB (front): the impulse pipeline — gather the top card ([CardSource.TopOfLibrary] one),
 *    [MoveCollectionEffect] to exile, then [Effects.GrantMayPlayFromExile] with
 *    [MayPlayExpiry.Permanent] gated by [YouControlSource]. The permission persists while the card
 *    stays exiled but is re-checked on every legal-action query, so the card is playable only while
 *    you still control the source permanent — "for as long as you control this creature." (Once the
 *    permanent leaves, its `ControllerComponent` is stripped and [YouControlSource] fails; the flip
 *    to Ghost-Spider keeps the same object, so control — and the grant — survive.) Same idiom as
 *    Hama, the Bloodbender's "for as long as you control Hama".
 *  - Cast-from-exile trigger (back): [Triggers.youCastSpell] gated by
 *    `SpellCastPredicate.CastFromZone(Zone.EXILE)` — the proven Quintorius Kand / Fire Lord Zuko
 *    idiom for "cast a spell from exile."
 *  - Play-a-land-from-exile trigger (back): a separate [ZoneChangeEvent] `EXILE → BATTLEFIELD`
 *    filtered to lands, `youControl()` — a land played from exile moves straight to the battlefield
 *    (lands never use the stack), so it fires the land branch while a creature spell cast from exile
 *    (exile → stack → battlefield) fires only the cast branch. Same enters-from-exile shape as Fire
 *    Lord Zuko's third ability, narrowed to lands.
 *  - Remove-two-counters ability (back): [Costs.RemoveCounterFromSelf] funding a
 *    [Patterns.Exile.impulse] — exile the top card and grant permission to play it this turn
 *    ([MayPlayExpiry.EndOfTurn]). Ghost-Spider only ever holds +1/+1 counters (its own trigger is
 *    the sole source), so the printed generic "two counters" is modeled as two +1/+1 counters —
 *    behaviorally identical in every state this card can reach, and the typed self-removal is the
 *    auto-paying activation-cost path (Peter Parker's Camera).
 */

private val GwenStacyFront = card("Gwen Stacy") {
    manaCost = "{1}{R}"
    colorIdentity = "RUW"
    typeLine = "Legendary Creature — Human Performer Hero"
    power = 2
    toughness = 1
    oracleText = "When Gwen Stacy enters, exile the top card of your library. You may play that " +
        "card for as long as you control this creature.\n" +
        "{2}{U}{R}{W}: Transform Gwen Stacy. Activate only as a sorcery."

    // When Gwen Stacy enters, exile the top card of your library. You may play that card for as
    // long as you control this creature.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                    storeAs = "gwenExiled",
                ),
                MoveCollectionEffect(
                    from = "gwenExiled",
                    destination = CardDestination.ToZone(Zone.EXILE),
                ),
                Effects.GrantMayPlayFromExile(
                    from = "gwenExiled",
                    expiry = MayPlayExpiry.Permanent,
                    condition = YouControlSource,
                ),
            )
        )
        description = "When Gwen Stacy enters, exile the top card of your library. You may play " +
            "that card for as long as you control this creature."
    }

    // {2}{U}{R}{W}: Transform Gwen Stacy. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{2}{U}{R}{W}")
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Gwen Stacy. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "78"
        artist = "Victor Adame Minguez"
        flavorText = "\"Spiders don't hold still given any other choice, and I have all the " +
            "choices in the Multiverse!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0f1597f-1dc7-465e-8fcb-0afe61bcca46.jpg?1783905342"
    }
}

private val GhostSpider = card("Ghost-Spider") {
    manaCost = ""
    colorIdentity = "RUW"
    colorIndicator = "RUW" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 4
    toughness = 4
    oracleText = "Flying, vigilance, haste\n" +
        "Whenever you play a land from exile or cast a spell from exile, put a +1/+1 counter on " +
        "Ghost-Spider.\n" +
        "Remove two counters from Ghost-Spider: Exile the top card of your library. You may play " +
        "that card this turn."

    keywords(Keyword.FLYING, Keyword.VIGILANCE, Keyword.HASTE)

    // Whenever you cast a spell from exile, put a +1/+1 counter on Ghost-Spider.
    triggeredAbility {
        trigger = Triggers.youCastSpell(
            requires = setOf(SpellCastPredicate.CastFromZone(Zone.EXILE)),
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you cast a spell from exile, put a +1/+1 counter on Ghost-Spider."
    }

    // Whenever you play a land from exile, put a +1/+1 counter on Ghost-Spider.
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Land,
                from = Zone.EXILE,
                to = Zone.BATTLEFIELD,
            ),
            binding = TriggerBinding.ANY,
        ).youControl()
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you play a land from exile, put a +1/+1 counter on Ghost-Spider."
    }

    // Remove two counters from Ghost-Spider: Exile the top card of your library. You may play that
    // card this turn.
    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(Counters.PLUS_ONE_PLUS_ONE, count = 2)
        effect = Patterns.Exile.impulse(count = 1, expiry = MayPlayExpiry.EndOfTurn)
        description = "Exile the top card of your library. You may play that card this turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "78"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/back/b/0/b0f1597f-1dc7-465e-8fcb-0afe61bcca46.jpg?1783905342"
    }
}

val GwenStacy: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = GwenStacyFront,
    backFace = GhostSpider,
)
