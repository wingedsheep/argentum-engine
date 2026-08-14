package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Convenient Target — Murders at Karlov Manor #119
 * {R} · Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, suspect enchanted creature.
 * Enchanted creature gets +1/+1.
 * {2}{R}: Return this card from your graveyard to your hand.
 *
 * A one-mana Aura that is genuinely playable on an *opponent's* creature as removal-adjacent tempo
 * (suspected creatures can't block) as well as on your own as a pump-plus-menace. Note the +1/+1
 * applies either way, so pointing it at an opponent is a real trade.
 *
 * Suspect is one-shot and **permanent**, not an Aura-bound continuous effect: per the printed
 * ruling, if the Aura later leaves the battlefield the creature stays suspected. So the suspect is
 * an enters trigger with [EffectTarget.EnchantedCreature] and `Duration.Permanent` (the default on
 * [Effects.Suspect]), not a static ability — modelling it statically would incorrectly un-suspect
 * the creature when the Aura died. Only the +1/+1 is the static half.
 *
 * The Aura enters attached to whatever it targeted on cast, so the enters trigger doesn't target
 * again — "enchanted creature" is a self-referential pointer, and an Aura that somehow entered
 * unattached simply has nothing to suspect.
 *
 * The graveyard ability makes the Aura's own death a recurring engine; it's `activateFromZone =
 * Zone.GRAVEYARD` at instant speed, which is how the card is printed (no "activate only as a
 * sorcery" clause).
 */
val ConvenientTarget = card("Convenient Target") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, suspect enchanted creature. (It has menace and can't block.)\n" +
        "Enchanted creature gets +1/+1.\n" +
        "{2}{R}: Return this card from your graveyard to your hand."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Suspect(EffectTarget.EnchantedCreature)
        description = "When this Aura enters, suspect enchanted creature."
    }

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EnchantedCreature)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Gaboleps"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d2cf2ae-9152-41c4-9dc4-a19da5812869.jpg?1783912885"

        ruling(
            "2024-02-02",
            "If Convenient Target leaves the battlefield, the creature it was enchanting will " +
                "still be suspected until that creature leaves the battlefield or another effect " +
                "causes it to no longer be suspected."
        )
        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until " +
                "it leaves the battlefield or another effect causes it to no longer be suspected."
        )
        ruling("2024-02-02", "If a creature is already suspected, suspecting it again won't have any effect.")
        ruling(
            "2024-02-02",
            "Being suspected isn't a copiable value. If a permanent becomes a copy of a suspected " +
                "creature, it won't be suspected."
        )
    }
}
