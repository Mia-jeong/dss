package io.github.ztkmkoo.dss.core.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.ReceiveBuilder;
import io.github.ztkmkoo.dss.core.actor.enumeration.DssNetworkCloseStatus;
import io.github.ztkmkoo.dss.core.message.DssNetworkCommand;
import io.github.ztkmkoo.dss.core.network.DssChannelProperty;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Objects;

/**
 * @author Kebron ztkmkoo@gmail.com
 * @create 2020-08-24 01:46
 */
public interface DssNetworkActor<P extends DssChannelProperty, S extends SocketChannel> extends DssActor<DssNetworkCommand>, DssMasterAcceptable, DssResolverAcceptable {

    P convertToChannelProperty(DssNetworkCommand.Bind msg);

    /**
     * Get ChannelInitializer for binding netty channel
     */
    ChannelInitializer<S> getChannelInitializer();

    /**
     * Get boss event loop group to make netty server bootstrap
     */
    EventLoopGroup getBossGroup();

    /**
     * Set boss event loop group to make netty server bootstrap
     */
    void setBossGroup(EventLoopGroup bossGroup);

    /**
     * Get worker event loop group to make netty server bootstrap
     */
    EventLoopGroup getWorkerGroup();

    /**
     * Set worker event loop group to make netty server bootstrap
     */
    void setWorkerGroup(EventLoopGroup workerGroup);

    boolean getActive();

    void setActive(boolean active);

    /**
     * make ReceiveBuilder for master actor with common handling message
     */
    default ReceiveBuilder<DssNetworkCommand> networkReceiveBuilder(AbstractDssActor<DssNetworkCommand> actor) {
        return actor.newReceiveBuilder()
                .onMessage(DssNetworkCommand.Bind.class, this::handlingBind)
                .onMessage(DssNetworkCommand.Close.class, this::handlingClose)
                ;
    }

    default Behavior<DssNetworkCommand> handlingBind(DssNetworkCommand.Bind msg) {
        if(getActive()) {
            getLog().warn("Network actor is already binding: {}", msg);
            return Behaviors.same();
        }

        final ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(getBossGroup(), getWorkerGroup());
        final P property = convertToChannelProperty(msg);
        final Channel channel;
        try {
            channel = bind(bootstrap, property, getChannelInitializer());
            channel.closeFuture().addListeners(closeFuture());
            setActive(true);
        } catch (Exception e) {
            getLog().error("Network actor bind error, ", e);
        }

        return Behaviors.same();
    }

    default Channel bind(ServerBootstrap serverBootstrap, P property, ChannelInitializer<S> channelInitializer) throws InterruptedException {
        Objects.requireNonNull(serverBootstrap);
        Objects.requireNonNull(property);
        Objects.requireNonNull(property.getServerSocketClass());
        Objects.requireNonNull(channelInitializer);

        return serverBootstrap
                .channel(property.getServerSocketClass())
                .handler(new LoggingHandler(property.getDssLogLevel().getNettyLogLevel()))
                .childHandler(channelInitializer)
                .bind(property.getHost(), property.getPort())
                .sync()
                .channel();
    }

    /**
     * Future of when netty channel is closed
     */
    default ChannelFutureListener closeFuture() {
        return future -> {
            if(Objects.nonNull(future)) {
                DssNetworkCloseStatus status = DssNetworkCloseStatus.from(future.isDone(), future.isSuccess(), future.isCancelled(), future.cause());
                switch (status) {
                    case CANCELLED:
                        // close cancelled
                        return;
                    case SUCCESSFULLY:
                    case FAILED:
                    case UNCOMPLETED:
                    default:
                        tellSelf(DssNetworkCommand.Close.INST);
                }
            }
        };
    }

    default void tellSelf(DssNetworkCommand msg) {
        Objects.requireNonNull(getContext());
        Objects.requireNonNull(getContext().getSelf());
        getContext().getSelf().tell(msg);
    }

    default Behavior<DssNetworkCommand> handlingClose(DssNetworkCommand.Close msg) {
        Objects.requireNonNull(msg);

        if (Objects.nonNull(getBossGroup())) {
            getBossGroup().shutdownGracefully();
            setBossGroup(null);
        }

        if (Objects.nonNull(getWorkerGroup())) {
            getWorkerGroup().shutdownGracefully();
            setWorkerGroup(null);
        }

        return Behaviors.stopped();
    }
}