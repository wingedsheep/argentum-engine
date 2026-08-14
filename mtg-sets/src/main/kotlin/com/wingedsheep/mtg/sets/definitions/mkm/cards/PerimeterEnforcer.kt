package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Perimeter Enforcer — Murders at Karlov Manor #31
 * {1}{W} · Creature — Human Detective 1/1
 *
 * Flying, lifelink
 * Whenever another Detective you control enters and whenever a Detective you control is turned face
 * up, this creature gets +1/+1 until end of turn.
 *
 * A 1/1 flying lifelinker that grows for the turn every time the Detective count goes up — the
 * white half of MKM's Detective tribal, and the payoff that makes the set's disguise creatures pull
 * double duty.
 *
 * The printed ability is one ability with **two** trigger conditions, which the engine models as
 * two `triggeredAbility` blocks with the same effect. That's not a fidelity compromise: two
 * separate triggers is exactly how the ability behaves — if a face-down Detective is turned face up
 * it fires once (it isn't entering), and both conditions can never be satisfied by the same event.
 *
 * The "enters" half is [TriggerBinding.OTHER] with a Detective-you-control filter, so the Enforcer's
 * own arrival doesn't pump it ("*another* Detective"). The "turned face up" half deliberately has
 * **no** "another" clause on the printed card, so it fires even when the Enforcer itself is the
 * creature turned face up — hence [Triggers.CreatureTurnedFaceUp] (ANY binding), not an OTHER one.
 * That filter is evaluated against the permanent's post-flip characteristics, which is the only
 * reading that works: a face-down creature is a nameless, typeless 2/2, so nothing would ever be a
 * Detective at the moment it's turned face up if the check read the face-down state.
 *
 * The pump is [EffectTarget.Self] with the default `Duration.EndOfTurn` — it stacks with itself
 * across multiple triggers in a turn and wears off in the cleanup step.
 */
val PerimeterEnforcer = card("Perimeter Enforcer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Detective"
    power = 1
    toughness = 1
    oracleText = "Flying, lifelink\n" +
        "Whenever another Detective you control enters and whenever a Detective you control is " +
        "turned face up, this creature gets +1/+1 until end of turn."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    // Whenever another Detective you control enters …
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.DETECTIVE).youControl(),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Whenever another Detective you control enters, this creature gets +1/+1 until end of turn."
    }

    // … and whenever a Detective you control is turned face up.
    triggeredAbility {
        trigger = Triggers.CreatureTurnedFaceUp(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.DETECTIVE)
        )
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Whenever a Detective you control is turned face up, this creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Josh Hass"
        flavorText = "He makes sure no one sets foot in a fresh crime scene—not even himself."
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f88d077-5082-4a67-91e4-97aafb9a5e91.jpg?1783912919"
    }
}
