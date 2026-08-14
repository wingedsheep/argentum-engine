package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Gandalf, Goblins' Bane // Flameshape — The Hobbit #96
 * {2}{R} · Legendary Creature — Avatar Wizard · Mythic
 * 2/3
 *
 * Whenever you cast a noncreature spell, Gandalf gets +1/+1 until end of turn and deals 1 damage
 * to each opponent.
 *
 * Adventure: Flameshape — {1}{R}, Sorcery — Adventure
 * Look at the top two cards of your library and exile them face down. For as long as they remain
 * exiled, you may play them if you control a Wizard.
 *
 * Modeling notes:
 *  - The cast trigger is one ability with two effects, so both halves happen (or neither) on a
 *    single resolution. The pump is [EffectTarget.Self]-scoped and end-of-turn bounded; the damage
 *    is a non-targeted hit on `Player.EachOpponent`, so it ignores hexproof and works in a pod.
 *  - Flameshape is the impulse pipeline with two riders the plain `impulse()` recipe can't express:
 *    the exiled cards are face down ([FaceDownMode.HIDDEN], so opponents never learn them), and the
 *    play permission never expires ([MayPlayExpiry.Permanent] — "for as long as they remain
 *    exiled") but is *gated* on controlling a Wizard. The gate is a `condition` on the grant, which
 *    the engine re-checks on every legal-action query, so losing your last Wizard suspends the
 *    permission and getting one back restores it — matching "you may play them **if** you control a
 *    Wizard" rather than a one-shot check on resolution.
 *  - The gather keeps the default controller-facing look overlay, which *is* the "look at the top
 *    two cards of your library" clause.
 *  - Note Gandalf himself is a Wizard, so casting the creature face later turns the permission on.
 *  - (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 *    caster cast it as the creature spell while it remains in exile.)
 */
val GandalfGoblinsBane = card("Gandalf, Goblins' Bane") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Avatar Wizard"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast a noncreature spell, Gandalf gets +1/+1 until end of turn and " +
        "deals 1 damage to each opponent."

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = Effects.Composite(
            Effects.ModifyStats(power = 1, toughness = 1, target = EffectTarget.Self),
            Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        )
    }

    adventure("Flameshape") {
        manaCost = "{1}{R}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Look at the top two cards of your library and exile them face down. For as " +
            "long as they remain exiled, you may play them if you control a Wizard. (Then exile " +
            "this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.Composite(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2)),
                    storeAs = "flameshapeExiled"
                ),
                MoveCollectionEffect(
                    from = "flameshapeExiled",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    faceDown = FaceDownMode.HIDDEN
                ),
                Effects.GrantMayPlayFromExile(
                    from = "flameshapeExiled",
                    expiry = MayPlayExpiry.Permanent,
                    condition = Conditions.YouControl(GameObjectFilter.Creature.withSubtype("Wizard"))
                )
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "96"
        artist = "Francisco Miyara"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b0d29a1-7da9-4fb3-8536-8ff8d8acae0b.jpg?1784376993"
    }
}
