/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

#import "RCManager.h"
#import "RCCustomChatAndVideoView.h"

@interface RCManager() {
    NSString *restCommUserName;
}

@property (nonatomic,retain) RCDevice* device;
@property (nonatomic,retain) RCConnection* connection;
@property NSMutableDictionary * parameters;
@property ARDVideoCallView *videoCallView;
@property RTCVideoTrack *localVideoTrack;
@property (nonatomic,retain) RCConnection* pendingIncomingConnection;
@end

@implementation RCManager

+ (RCManager*)sharedInstance
{
    // 1
    static RCManager *_sharedInstance = nil;
    
    // 2
    static dispatch_once_t oncePredicate;
    
    // 3
    dispatch_once(&oncePredicate, ^{
        _sharedInstance = [[RCManager alloc] init];
    });
    return _sharedInstance;
}
- (id)init
{
    self = [super init];
    if (self) {
        _isVideoChatInProgress = NO;
    }
    return self;
}
- (void)registerWithUserName:(NSString *)userName password:(NSString *)password
{
    restCommUserName = userName;
    self.parameters = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                       userName, AOR,
                       password, PASSWORD,
                       nil];
    // CHANGEME: set the IP address of your RestComm instance in the URI below
    [self.parameters setObject:[NSString stringWithFormat:@"sip:%@",self.serverURL]
                        forKey:REGISTRAR];
    // initialize RestComm Client by setting up an RCDevice
    self.device = [[RCDevice alloc] initWithParams:self.parameters delegate:self];
    NSLog(@"%@",self.device);
    self.videoCallView = [[ARDVideoCallView alloc] initWithFrame:CGRectMake(0, 0, 200, 200)];
    self.connection = nil;
}
-(void)connectToRC
{
    [self.parameters setObject:MAKECALL forKey:INVOKE_VIEW_TYPE];
    // To whom u call is configured in settings initially
    [self.parameters setObject:[NSString stringWithFormat:@"sip:%@@%@",self.agentName,self.serverURL] forKey:USERNAME];
    [self.parameters setObject:[NSNumber numberWithBool:YES] forKey:VIDEO_ENABLED];
    // call the other party
    self.connection = [self.device connect:self.parameters delegate:self];
}
// 'ringing' for incoming connections -let's animate the 'Answer' button to give a hint to the user
- (void)device:(RCDevice*)device didReceiveIncomingConnection:(RCConnection*)connection
{
    [self.parameters setObject:RECEIVE_CALL forKey:INVOKE_VIEW_TYPE];
    // @todo:For now the call is always between "Alice" and bob
    [self.parameters setObject:[NSString stringWithFormat:@"sip:%@@%@",restCommUserName,self.serverURL] forKey:USERNAME];
    self.pendingIncomingConnection = connection;
    [[NSNotificationCenter defaultCenter]postNotificationName:RCIncomingCall object:nil];
}
-(void)answerVideoPressed
{
    [self answer:YES];
}
- (void)answer:(BOOL)allowVideo
{
    if (self.pendingIncomingConnection)
    {
        if (allowVideo)
        {
            [self.pendingIncomingConnection accept:[NSDictionary dictionaryWithObject:[NSNumber numberWithBool:YES]forKey:VIDEO_ENABLED]];
        }
        else
        {
            [self.pendingIncomingConnection accept:[NSDictionary dictionaryWithObject:[NSNumber numberWithBool:NO]forKey:VIDEO_ENABLED]];
        }
        self.connection = self.pendingIncomingConnection;
    }
}
-(void)diclineIncomingCall
{
    if (self.pendingIncomingConnection)
    {
        [self.pendingIncomingConnection reject];
        self.pendingIncomingConnection = nil;
    }
}
 #pragma mark - <RCDeviceDelegates>
- (void)deviceDidInitializeSignaling:(RCDevice *)device
{
    self.isRegistered = YES;
    self.isInitialized = YES;
    [[NSNotificationCenter defaultCenter] postNotificationName:RCInitialisationComplete object:nil];
}
- (void)deviceDidStartListeningForIncomingConnections:(RCDevice*)device
{
    
}
- (void)device:(RCDevice *)device didReceivePresenceUpdate:(RCPresenceEvent *)presenceEvent
{
    
}
- (void)device:(RCDevice*)device didStopListeningForIncomingConnections:(NSError*)error
{
    
}
- (void)device:(RCDevice *)device didReceiveIncomingMessage:(NSString *)message withParams:(NSDictionary *)params
{
    NSLog(@"%@",message);
    NSMutableDictionary *parameters = [[NSMutableDictionary alloc] init];
    [parameters setObject:message forKey:MESSAGE_TEXT];
    [parameters setObject:RECEIVE_MESSAGE forKey:INVOKE_VIEW_TYPE];
    [parameters setObject:[params objectForKey:FROM] forKey:USERNAME];
    if ([message isEqualToString:RCChatClosed]) {
        [[NSNotificationCenter defaultCenter] postNotificationName:RCChatClosed object:self];
    }
    else
    {
        [[NSNotificationCenter defaultCenter] postNotificationName:RCIncomingMessage object:nil userInfo:parameters];
    }
}
 #pragma mark - <RCConnectionDelegate>
- (void)connectionDidStartConnecting:(RCConnection*)connection
{
    NSLog(@"connectionDidStartConnecting");
}
- (void)connectionDidConnect:(RCConnection*)connection
{
    NSLog(@"connectionDidConnect");
}
- (void)connection:(RCConnection *)connection didReceiveRemoteVideo:(RTCVideoTrack *)remoteVideoTrack
{
    NSLog(@"didReceiveRemoteVideo");
    _isVideoChatInProgress = YES;
    if (!self.remoteVideoTrack) {
        self.remoteVideoTrack = remoteVideoTrack;
        [self.remoteVideoTrack addRenderer:self.videoCallView.remoteVideoView];
        [[NSNotificationCenter defaultCenter] postNotificationName:RCRemoteVideoReceived object:nil];
    }
}
- (void)connection:(RCConnection *)connection didReceiveLocalVideo:(RTCVideoTrack *)localVideoTrack
{
    if (!self.localVideoTrack)
    {
        self.localVideoTrack = localVideoTrack;
        [self.localVideoTrack addRenderer:self.videoCallView.localVideoView];
    }
}
- (void)setMuted:(BOOL)muted
{
    self.connection.muted = muted;
}

-(void)sendMessage:(NSString *)message
{
    [self.parameters setObject:[NSString stringWithFormat:@"sip:%@@%@",self.agentName,self.serverURL] forKey:USERNAME];
    [self.device sendMessage:message
                          to:[NSDictionary dictionaryWithObject:[self.parameters objectForKey:USERNAME] forKey:USERNAME]];
}
 #pragma mark - <RCConnectionDelegates>
- (void)connectionDidCancel:(RCConnection*)connection
{
    NSLog(@"connectionDidCancel");
    
    if (self.pendingIncomingConnection) {
        self.pendingIncomingConnection = nil;
        self.connection = nil;
        [self stopVideoRendering];
    }
}
- (void)connectionDidDisconnect:(RCConnection*)connection
{
    NSLog(@"connectionDidDisconnect");
    self.connection = nil;
    self.pendingIncomingConnection = nil;
    [self stopVideoRendering];
}
- (void)connectionDidGetDeclined:(RCConnection*)connection
{
    NSLog(@"connectionDidGetDeclined");
    self.connection = nil;
    self.pendingIncomingConnection = nil;
    [self stopVideoRendering];
    [[NSNotificationCenter defaultCenter] postNotificationName:RCConnectionConnectionDeclined object:self];
}
- (void)connection:(RCConnection*)connection didFailWithError:(NSError*)error
{
    NSLog(@"%@",error);
}
- (void)disconnect
{
    if (self.connection)
    {
        [self.connection disconnect];
    }
    [self stopVideoRendering];
}
- (void)stopVideoRendering
{
    if (self.remoteVideoTrack)
    {
        [self.remoteVideoTrack removeRenderer:self.videoCallView.remoteVideoView];
        self.remoteVideoTrack = nil;
        [self.videoCallView.remoteVideoView renderFrame:nil];
    }
    if (self.localVideoTrack)
    {
        [self.localVideoTrack removeRenderer:self.videoCallView.localVideoView];
        self.localVideoTrack = nil;
        [self.videoCallView.localVideoView renderFrame:nil];
    }
    [[NSNotificationCenter defaultCenter] postNotificationName:RCConnectionDidDisconnect object:self];
}
- (ARDVideoCallView *)getVideoChatViewWithFrame:(CGRect)videoFrame
{
    self.videoCallView.frame = videoFrame;
    return self.videoCallView;
}
-(RCCustomChatAndVideoView *)getRCCustomChatView:(CGRect)instantMessageFrame
{
    self.mChatView=[RCCustomChatAndVideoView customMessageViewFrame:instantMessageFrame];
    return self.mChatView;
}
-(RCCustomChatAndVideoView *)getRCCustomVideoChatView:(CGRect)videoChatViewFrame toEmbedInClientVideoView:(BOOL)embedInClientVideoView
{
    self.mChatView=[RCCustomChatAndVideoView customVideoViewFrame:videoChatViewFrame :embedInClientVideoView];
    [self.mChatView addSubview:[self getVideoChatViewWithFrame:CGRectMake(0,0,self.mChatView.frame.size.width,self.mChatView.frame.size.height-35)]];
    return self.mChatView;
}
@end