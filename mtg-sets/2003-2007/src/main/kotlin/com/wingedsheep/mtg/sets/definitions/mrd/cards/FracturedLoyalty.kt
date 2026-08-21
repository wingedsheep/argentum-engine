package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fractured Loyalty — Mirrodin #93 (canonical printing)
 * {1}{R} · Enchantment — Aura
 *
 * Enchant creature
 * Whenever enchanted creature becomes the target of a spell or ability, that spell or ability's
 * controller gains control of that creature. (This effect lasts indefinitely.)
 *
 * The Aura punishes *attention*, not damage: any targeting at all — a removal spell, a pump spell,
 * an equip-style ability — hands the creature to whoever pointed at it. That is why the trigger is
 * the plain [Triggers.BecomesTarget] with no spell/ability narrowing and no "an opponent controls"
 * clause: targeting your own enchanted creature gives it to *you*, which is the card's whole
 * bargaining position.
 *
 * Two script choices worth stating, because the obvious spellings are both wrong:
 *
 * - The trigger is **ANY-bound over `attachedToBySource()`**, not `TriggerBinding.ATTACHED`.
 *   `BecomesTargetEvent` is one of the events `AttachmentTriggerDetector` does not route, so an
 *   ATTACHED binding would silently never fire. Matching "a creature this Aura is attached to"
 *   as a filter reaches the same set through the path that does work.
 * - The effect moves [EffectTarget.TriggeringEntity], not `EnchantedCreature`. The card says "that
 *   creature", and the two diverge for real: this trigger goes on the stack *above* the spell that
 *   caused it, so anyone may respond by destroying the Aura. By resolution there is then no
 *   enchanted creature to name — while "that creature" is still on the battlefield and still
 *   changes hands, which is what the printed text says happens.
 *
 * The new controller is [Player.ControllerOfTargetingSource], the reference this card motivated:
 * a becomes-target trigger binds the *targeted* object as its triggering entity, so nothing in the
 * existing player vocabulary could reach back to the other end of the targeting.
 */
val FracturedLoyalty = card("Fractured Loyalty") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature becomes the target of a spell or ability, that spell or " +
        "ability's controller gains control of that creature. (This effect lasts indefinitely.)"

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.BecomesTarget(GameObjectFilter.Creature.attachedToBySource())
        effect = GiveControlToTargetPlayerEffect(
            permanent = EffectTarget.TriggeringEntity,
            newController = EffectTarget.PlayerRef(Player.ControllerOfTargetingSource)
        )
        description = "Whenever enchanted creature becomes the target of a spell or ability, " +
            "that spell or ability's controller gains control of that creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c7ce5d5-e51a-4dbc-82f2-b79c88769a7b.jpg?1783944541"
    }
}
