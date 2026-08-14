package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Eddie Brock // Venom, Lethal Protector — Marvel's Spider-Man #55 (mythic)
 *
 * Front — Eddie Brock · {2}{B} · Legendary Creature — Human Hero Villain · 3/3
 *   When Eddie Brock enters, return target creature card with mana value 1 or less from your
 *   graveyard to the battlefield.
 *   {3}{B}{R}{G}: Transform Eddie Brock. Activate only as a sorcery.
 *
 * Back — Venom, Lethal Protector · Legendary Creature — Symbiote Hero Villain · 5/5
 *   Menace, trample, haste
 *   Whenever Venom attacks, you may sacrifice another creature. If you do, draw X cards, then you
 *   may put a permanent card with mana value X or less from your hand onto the battlefield, where
 *   X is the sacrificed creature's mana value.
 *
 * Modeled as a transforming double-faced creature ([CardDefinition.doubleFacedCreature]); the
 * front owns the sorcery-speed [TransformEffect] flip. The back is a transformed face reached only
 * via the flip, so it carries no castable mana cost — its B/R/G colors come from a color indicator
 * (CR 204), `colorIndicator = "BRG"` — mirroring the shipped Miles Morales // Ultimate Spider-Man.
 *
 *  - ETB (front): a mandatory [Effects.Move] `GRAVEYARD → BATTLEFIELD` reanimation of a single
 *    target creature card in your graveyard, restricted to `manaValueAtMost(1)` (the same
 *    graveyard-target idiom as Reya Dawnbringer / Daily Bugle Reporters).
 *  - Attack trigger (back): [Triggers.Attacks] + [MayEffect] wrapping the optional sacrifice of
 *    another creature ([Effects.SacrificeTarget] over a `.other()` creature you control), so "If
 *    you do" gates the payoff on actually sacrificing. The sacrificed creature's mana value is read
 *    from last-known information via [EntityReference.Sacrificed] — the same capture Memorial Vault
 *    / Eldritch Evolution rely on — and feeds two downstream reads: the draw count
 *    ([DynamicAmount.EntityProperty] `Sacrificed.ManaValue`) and the from-hand eligibility filter
 *    (`GameObjectFilter.Permanent.manaValueAtMostEntity(Sacrificed)`). The "you may put a permanent
 *    card … onto the battlefield" is the gather → select → move pipeline with a
 *    [SelectionMode.ChooseUpTo]`(1)` select supplying the optionality (choose 0 or 1). The draw
 *    happens before the select, so freshly-drawn cards are eligible to be put onto the battlefield.
 */

private val EddieBrockFront = card("Eddie Brock") {
    manaCost = "{2}{B}"
    colorIdentity = "BRG"
    typeLine = "Legendary Creature — Human Hero Villain"
    power = 3
    toughness = 3
    oracleText = "When Eddie Brock enters, return target creature card with mana value 1 or less " +
        "from your graveyard to the battlefield.\n" +
        "{3}{B}{R}{G}: Transform Eddie Brock. Activate only as a sorcery."

    // When Eddie Brock enters, return target creature card with mana value 1 or less from your
    // graveyard to the battlefield.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val reanimated = target(
            "target creature card with mana value 1 or less from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Creature.ownedByYou().manaValueAtMost(1),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.Move(reanimated, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        description = "When Eddie Brock enters, return target creature card with mana value 1 or " +
            "less from your graveyard to the battlefield."
    }

    // {3}{B}{R}{G}: Transform Eddie Brock. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{3}{B}{R}{G}")
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Eddie Brock. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "55"
        artist = "Greg Staples"
        flavorText = "\"I've got the nose for the story. He's got the hunger for the kill. " +
            "Together...\""
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3455651-e643-445e-9489-51e4e24fca4c.jpg?1783905351"
    }
}

private val VenomLethalProtector = card("Venom, Lethal Protector") {
    manaCost = ""
    colorIdentity = "BRG"
    colorIndicator = "BRG" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Creature — Symbiote Hero Villain"
    power = 5
    toughness = 5
    oracleText = "Menace, trample, haste\n" +
        "Whenever Venom attacks, you may sacrifice another creature. If you do, draw X cards, " +
        "then you may put a permanent card with mana value X or less from your hand onto the " +
        "battlefield, where X is the sacrificed creature's mana value."

    keywords(Keyword.MENACE, Keyword.TRAMPLE, Keyword.HASTE)

    // Whenever Venom attacks, you may sacrifice another creature. If you do, draw X cards, then you
    // may put a permanent card with mana value X or less from your hand onto the battlefield, where
    // X is the sacrificed creature's mana value.
    triggeredAbility {
        trigger = Triggers.Attacks
        val sacrificed = target(
            "another creature",
            TargetPermanent(
                filter = TargetFilter(GameObjectFilter.Creature.youControl()).other()
            )
        )
        // X = the sacrificed creature's mana value (last-known info via EntityReference.Sacrificed).
        val x = DynamicAmount.EntityProperty(
            EntityReference.Sacrificed(0),
            EntityNumericProperty.ManaValue
        )
        effect = MayEffect(
            Effects.SacrificeTarget(sacrificed) then
                Effects.DrawCards(x) then
                Effects.Composite(
                    listOf(
                        // Gather every permanent card in hand; the mana-value cap is enforced by
                        // the selection restriction below rather than a card filter, because a
                        // card-filter dynamic cap cannot read the sacrificed permanent's LKI
                        // snapshot (its dynamic-cap evaluation runs in a stripped context), whereas
                        // a SelectionRestriction resolves its amount against the full effect context.
                        GatherCardsEffect(
                            source = CardSource.FromZone(
                                Zone.HAND,
                                Player.You,
                                GameObjectFilter.Permanent
                            ),
                            storeAs = "venomHand"
                        ),
                        // "you may put a permanent card with mana value X or less" — choose up to one
                        // whose mana value is at most X (the sacrificed creature's mana value).
                        SelectFromCollectionEffect(
                            from = "venomHand",
                            selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                            restrictions = listOf(SelectionRestriction.TotalManaValueAtMost(maxAmount = x)),
                            storeSelected = "venomChosen",
                            prompt = "You may put a permanent card with mana value X or less onto " +
                                "the battlefield"
                        ),
                        MoveCollectionEffect(
                            from = "venomChosen",
                            destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You)
                        )
                    )
                )
        )
        description = "Whenever Venom attacks, you may sacrifice another creature. If you do, draw " +
            "X cards, then you may put a permanent card with mana value X or less from your hand " +
            "onto the battlefield, where X is the sacrificed creature's mana value."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "55"
        artist = "Greg Staples"
        flavorText = "\"... we are Venom!\""
        imageUri = "https://cards.scryfall.io/normal/back/f/3/f3455651-e643-445e-9489-51e4e24fca4c.jpg?1783905351"
    }
}

val EddieBrock: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = EddieBrockFront,
    backFace = VenomLethalProtector,
)
